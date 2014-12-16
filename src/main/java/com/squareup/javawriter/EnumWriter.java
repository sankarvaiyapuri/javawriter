/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javawriter;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public final class EnumWriter extends TypeWriter {
  public static EnumWriter forClassName(ClassName name) {
    checkArgument(name.enclosingSimpleNames().isEmpty(), "%s must be top-level type.", name);
    return new EnumWriter(name);
  }

  private final Map<String, ConstantWriter> constantWriters = Maps.newLinkedHashMap();

  EnumWriter(ClassName name) {
    super(name);
  }

  public ConstantWriter addConstant(String name) {
    ConstantWriter constantWriter = new ConstantWriter(name);
    constantWriters.put(name, constantWriter);
    return constantWriter;
  }

  public ConstructorWriter addConstructor() {
    return body.addConstructor();
  }

  @Override
  public Appendable write(Appendable appendable, Context context) throws IOException {
    checkState(!constantWriters.isEmpty(), "Cannot write an enum with no constants.");

    context = createSubcontext(context);
    writeAnnotations(appendable, context, '\n');
    writeModifiers(appendable).append("enum ").append(name.simpleName());
    Writables.Joiner.on(", ").prefix(" implements ")
        .appendTo(appendable, context, implementedTypes);
    appendable.append(" {\n");

    Writables.Joiner.on(",\n")
        .appendTo(new IndentingAppendable(appendable), context, constantWriters.values());
    appendable.append(";\n");

    body.write(appendable, context);
    appendable.append("}\n");
    return appendable;
  }

  @Override
  public Set<ClassName> referencedClasses() {
    Iterable<? extends HasClassReferences> concat =
        Iterables.concat(super.referencedClasses(), constantWriters.values());
    return FluentIterable.from(concat)
        .transformAndConcat(GET_REFERENCED_CLASSES)
        .toSet();
  }

  public static final class ConstantWriter implements Writable, HasClassReferences {
    private final String name;
    private final List<Snippet> constructorSnippets;
    private final ClassBodyWriter body;

    private ConstantWriter(String name) {
      this.name = name;
      this.constructorSnippets = Lists.newArrayList();
      this.body = ClassBodyWriter.forAnonymousType();
    }

    public ConstantWriter addArgument(Snippet snippet) {
      constructorSnippets.add(snippet);
      return this;
    }

    public MethodWriter addMethod(TypeWriter returnType, String name) {
      return body.addMethod(returnType, name);
    }

    public MethodWriter addMethod(TypeMirror returnType, String name) {
      return body.addMethod(returnType, name);
    }

    public MethodWriter addMethod(TypeName returnType, String name) {
      return body.addMethod(returnType, name);
    }

    public MethodWriter addMethod(Class<?> returnType, String name) {
      return body.addMethod(returnType, name);
    }

    public FieldWriter addField(Class<?> type, String name) {
      return body.addField(type, name);
    }

    public FieldWriter addField(TypeElement type, String name) {
      return body.addField(type, name);
    }

    public FieldWriter addField(TypeName type, String name) {
      return body.addField(type, name);
    }

    @Override
    public Appendable write(Appendable appendable, Context context) throws IOException {
      appendable.append(name);
      Writables.Joiner.on(", ").wrap("(", ")").appendTo(appendable, context, constructorSnippets);
      if (!body.isEmpty()) {
        appendable.append(" {");
        body.write(appendable, context);
        appendable.append('}');
      }
      return appendable;
    }

    @Override
    public Set<ClassName> referencedClasses() {
      return FluentIterable.from(Iterables.concat(constructorSnippets, ImmutableList.of(body)))
          .transformAndConcat(GET_REFERENCED_CLASSES)
          .toSet();
    }
  }
}
