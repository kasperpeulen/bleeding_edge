// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of ssa;

abstract class HType {
  const HType();

  /**
   * Returns an [HType] with the given type mask. The factory method
   * takes care to track whether or not the resulting type may be a
   * primitive type.
   */
  factory HType.fromMask(TypeMask mask, Compiler compiler) {
    bool isNullable = mask.isNullable;
    JavaScriptBackend backend = compiler.backend;
    if (mask.isEmpty) {
      return isNullable ? backend.nullType : backend.emptyType;
    }

    if (mask.containsOnlyInt(compiler)) {
      return mask.isNullable
          ? new HBoundedType(new TypeMask.exact(backend.jsIntClass))
          : backend.intType;
    } else if (mask.containsOnlyDouble(compiler)) {
      return mask.isNullable
          ? new HBoundedType(new TypeMask.exact(backend.jsDoubleClass))
          : backend.doubleType;
    } else if (mask.containsOnlyNum(compiler)
               || mask.satisfies(backend.jsNumberClass, compiler)) {
      return mask.isNullable
          ? new HBoundedType(new TypeMask.subclass(backend.jsNumberClass))
          : backend.numType;
    } else if (mask.containsOnlyString(compiler)) {
      // TODO(ngeoffray): Avoid creating [TypeMask]s with the string
      // class as base.
      return isNullable
          ? new HBoundedType(new TypeMask.exact(backend.jsStringClass))
          : backend.stringType;
    } else if (mask.containsOnlyBool(compiler)) {
      return isNullable
          ? new HBoundedType(new TypeMask.exact(backend.jsBoolClass))
          : backend.boolType;
    } else if (mask.containsOnlyNull(compiler)) {
      return backend.nullType;
    }

    // TODO(kasperl): A lot of the code in the system currently
    // expects the top type to be 'unknown'. I'll rework this.
    if (mask.containsAll(compiler)) {
      return isNullable ? backend.dynamicType : backend.nonNullType;
    }

    return new HBoundedType(mask);
  }

  factory HType.exact(ClassElement type, Compiler compiler) {
    TypeMask mask = new TypeMask.exact(type.declaration);
    return new HType.fromMask(mask, compiler);
  }

  factory HType.subclass(ClassElement type, Compiler compiler) {
    TypeMask mask = new TypeMask.subclass(type.declaration);
    return new HType.fromMask(mask, compiler);
  }

  factory HType.subtype(ClassElement type, Compiler compiler) {
    TypeMask mask = new TypeMask.subtype(type.declaration);
    return new HType.fromMask(mask, compiler);
  }

  factory HType.nonNullExact(ClassElement type, Compiler compiler) {
    TypeMask mask = new TypeMask.nonNullExact(type.declaration);
    return new HType.fromMask(mask, compiler);
  }

  factory HType.nonNullSubclass(ClassElement type, Compiler compiler) {
    TypeMask mask = new TypeMask.nonNullSubclass(type.declaration);
    return new HType.fromMask(mask, compiler);
  }

  factory HType.nonNullSubtype(ClassElement type, Compiler compiler) {
    TypeMask mask = new TypeMask.nonNullSubtype(type.declaration);
    return new HType.fromMask(mask, compiler);
  }

  factory HType.fromInferredType(TypeMask mask, Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    if (mask == null) return backend.dynamicType;
    return new HType.fromMask(mask, compiler);
  }

  factory HType.inferredReturnTypeForElement(
      Element element, Compiler compiler) {
    return new HType.fromInferredType(
        compiler.typesTask.getGuaranteedReturnTypeOfElement(element),
        compiler);
  }

  factory HType.inferredTypeForElement(Element element, Compiler compiler) {
    return new HType.fromInferredType(
        compiler.typesTask.getGuaranteedTypeOfElement(element),
        compiler);
  }

  factory HType.inferredTypeForSelector(Selector selector, Compiler compiler) {
    return new HType.fromInferredType(
        compiler.typesTask.getGuaranteedTypeOfSelector(selector),
        compiler);
  }

  factory HType.inferredForNode(Element owner, Node node, Compiler compiler) {
    return new HType.fromInferredType(
        compiler.typesTask.getGuaranteedTypeOfNode(owner, node),
        compiler);
  }

  factory HType.fromNativeBehavior(native.NativeBehavior nativeBehavior,
                                   Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    if (nativeBehavior.typesReturned.isEmpty) return backend.dynamicType;

    HType result = nativeBehavior.typesReturned
        .map((type) => fromNativeType(type, compiler))
        .reduce((t1, t2) => t1.union(t2, compiler));
    assert(!result.isConflicting());
    return result;
  }

  // [type] is either an instance of [DartType] or special objects
  // like [native.SpecialType.JsObject].
  static HType fromNativeType(type, Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    if (type == native.SpecialType.JsObject) {
      return new HType.nonNullExact(compiler.objectClass, compiler);
    } else if (type.isVoid) {
      return backend.nullType;
    } else if (type.element == compiler.nullClass) {
      return backend.nullType;
    } else if (type.treatAsDynamic) {
      return backend.dynamicType;
    } else if (compiler.world.hasAnySubtype(type.element)) {
      return new HType.nonNullSubtype(type.element, compiler);
    } else if (compiler.world.hasAnySubclass(type.element)) {
      return new HType.nonNullSubclass(type.element, compiler);
    } else {
      return new HType.nonNullExact(type.element, compiler);
    }
  }

  bool isConflicting() => false;
  bool isExact() => false;
  bool isNull() => false;
  bool isBoolean(Compiler compiler) => false;
  bool isNumber(Compiler compiler) => false;
  bool isInteger(Compiler compiler) => false;
  bool isDouble(Compiler compiler) => false;

  bool isString(Compiler compiler) => false;
  bool isFixedArray(Compiler compiler) => false;
  bool isReadableArray(Compiler compiler) => false;
  bool isMutableArray(Compiler compiler) => false;
  bool isExtendableArray(Compiler compiler) => false;
  bool isPrimitive(Compiler compiler) => false;
  bool isPrimitiveOrNull(Compiler compiler) => false;

  bool isBooleanOrNull(Compiler compiler) => false;
  bool isNumberOrNull(Compiler compiler) => false;
  bool isIntegerOrNull(Compiler compiler) => false;
  bool isDoubleOrNull(Compiler compiler) => false;

  // TODO(kasperl): Get rid of this one.
  bool isIndexablePrimitive(Compiler compiler) => false;

  bool isIndexable(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return implementsInterface(backend.jsIndexableClass, compiler);
  }

  bool isMutableIndexable(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return implementsInterface(backend.jsMutableIndexableClass, compiler);
  }

  bool implementsInterface(ClassElement interfaceElement, Compiler compiler) {
    TypeMask mask = new TypeMask.subtype(interfaceElement);
    return mask == mask.union(computeMask(compiler), compiler);
  }

  bool canBeNull() => false;
  bool canBePrimitive(Compiler compiler) => false;
  bool canBePrimitiveNumber(Compiler compiler) => false;
  bool canBePrimitiveString(Compiler compiler) => false;
  bool canBePrimitiveArray(Compiler compiler) => false;
  bool canBePrimitiveBoolean(Compiler compiler) => false;

  /** Alias for isReadableArray. */
  bool isArray(Compiler compiler) => isReadableArray(compiler);

  TypeMask computeMask(Compiler compiler);

  Selector refine(Selector selector, Compiler compiler) {
    // TODO(kasperl): Should we check if the refinement really is more
    // specialized than the starting point?
    TypeMask mask = computeMask(compiler);
    if (selector.mask == mask) return selector;
    return new TypedSelector(mask, selector);
  }

  /**
   * The intersection of two types is the intersection of its values. For
   * example:
   *   * INTEGER.intersect(NUMBER) => INTEGER.
   *   * DOUBLE.intersect(INTEGER) => CONFLICTING.
   *   * MUTABLE_ARRAY.intersect(READABLE_ARRAY) => MUTABLE_ARRAY.
   *
   * When there is no predefined type to represent the intersection returns
   * [CONFLICTING].
   *
   * An intersection with [UNKNOWN] returns the non-UNKNOWN type. An
   * intersection with [CONFLICTING] returns [CONFLICTING].
   */
  HType intersection(HType other, Compiler compiler) {
    TypeMask mask = computeMask(compiler);
    TypeMask otherMask = other.computeMask(compiler);
    TypeMask intersection = mask.intersection(otherMask, compiler);
    return new HType.fromMask(intersection, compiler);
  }

  /**
   * The union of two types is the union of its values. For example:
   *   * INTEGER.union(NUMBER) => NUMBER.
   *   * DOUBLE.union(INTEGER) => NUMBER.
   *   * MUTABLE_ARRAY.union(READABLE_ARRAY) => READABLE_ARRAY.
   *
   * When there is no predefined type to represent the union returns
   * [UNKNOWN].
   *
   * A union with [UNKNOWN] returns [UNKNOWN].
   * A union of [CONFLICTING] with any other types returns the other type.
   */
  HType union(HType other, Compiler compiler) {
    TypeMask mask = computeMask(compiler);
    TypeMask otherMask = other.computeMask(compiler);
    TypeMask union = mask.union(otherMask, compiler);
    return new HType.fromMask(union, compiler);
  }

  HType simplify(Compiler compiler) => this;

  HType nonNullable(compiler) {
    return new HType.fromMask(computeMask(compiler).nonNullable(), compiler);
  }

  bool containsAll(Compiler compiler) {
    return computeMask(compiler).containsAll(compiler);
  }
}

class HBoundedType extends HType {
  final TypeMask mask;
  const HBoundedType(this.mask);

  bool isExact() => mask.isExact || isNull();

  bool canBeNull() => mask.isNullable;

  bool isNull() => mask.isEmpty && mask.isNullable;
  bool isConflicting() => mask.isEmpty && !mask.isNullable;

  bool canBePrimitive(Compiler compiler) {
    return canBePrimitiveNumber(compiler)
        || canBePrimitiveArray(compiler)
        || canBePrimitiveBoolean(compiler)
        || canBePrimitiveString(compiler)
        || isNull();
  }

  bool canBePrimitiveNumber(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.contains(backend.jsNumberClass, compiler)
        || mask.contains(backend.jsIntClass, compiler)
        || mask.contains(backend.jsDoubleClass, compiler);
  }

  bool canBePrimitiveBoolean(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.contains(backend.jsBoolClass, compiler);
  }

  bool canBePrimitiveArray(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.contains(backend.jsArrayClass, compiler)
        || mask.contains(backend.jsFixedArrayClass, compiler)
        || mask.contains(backend.jsExtendableArrayClass, compiler);
  }

  bool isIndexablePrimitive(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.satisfies(backend.jsIndexableClass, compiler);
  }

  bool isFixedArray(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.containsOnly(backend.jsFixedArrayClass);
  }

  bool isExtendableArray(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.containsOnly(backend.jsExtendableArrayClass);
  }

  bool isMutableArray(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.satisfies(backend.jsMutableArrayClass, compiler);
  }

  bool isReadableArray(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.satisfies(backend.jsArrayClass, compiler);
  }

  bool canBePrimitiveString(Compiler compiler) {
    JavaScriptBackend backend = compiler.backend;
    return mask.contains(backend.jsStringClass, compiler);
  }

  TypeMask computeMask(Compiler compiler) => mask;

  HType simplify(Compiler compiler) {
    return new HType.fromMask(mask.simplify(compiler), compiler);
  }

  bool isInteger(Compiler compiler) {
    return mask.containsOnlyInt(compiler) && !mask.isNullable;
  }

  bool isIntegerOrNull(Compiler compiler) {
    return mask.containsOnlyInt(compiler);
  }

  bool isNumber(Compiler compiler) {
    return mask.containsOnlyNum(compiler) && !mask.isNullable;
  }

  bool isNumberOrNull(Compiler compiler) {
    return mask.containsOnlyNum(compiler);
  }

  bool isDouble(Compiler compiler) {
    return mask.containsOnlyDouble(compiler) && !mask.isNullable;
  }

  bool isDoubleOrNull(Compiler compiler) {
    return mask.containsOnlyDouble(compiler);
  }

  bool isBoolean(Compiler compiler) {
    return mask.containsOnlyBool(compiler) && !mask.isNullable;
  }

  bool isBooleanOrNull(Compiler compiler) {
    return mask.containsOnlyBool(compiler);
  }

  bool isString(Compiler compiler) {
    return mask.containsOnlyString(compiler);
  }

  bool isPrimitive(Compiler compiler) {
    return (isPrimitiveOrNull(compiler) && !mask.isNullable)
        || isNull();
  }

  bool isPrimitiveOrNull(Compiler compiler) {
    return isIndexablePrimitive(compiler)
        || isNumberOrNull(compiler)
        || isBooleanOrNull(compiler)
        || isNull();
  }

  bool operator ==(other) {
    if (other is !HBoundedType) return false;
    HBoundedType bounded = other;
    return mask == bounded.mask;
  }

  int get hashCode => mask.hashCode;

  String toString() {
    return 'BoundedType(mask=$mask)';
  }
}
