/**
 * Copyright (c) 2015, Mollie B.V.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.mollie.util;

import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.smarty.lang.SmartyTokenTypes;
import com.jetbrains.smarty.lang.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Static helper to find Smarty and PHP functions.
 */
public class SmartyIndex
{
	protected static final Logger LOG = Logger.getInstance(SmartyIndex.class);

	/**
	 * Get a list of all the Smarty 'block' plugins in the current project.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 *
	 * @return List of all Smarty 'block' plugins.
	 */
	public static Collection<String> getSmartyBlockPluginNames (Project project, String prefix)
	{
		return getSmartyPluginNamesOfType(project, prefix, "block");
	}

	/**
	 * Get a list of all the Smarty 'function' plugins in the current project.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 *
	 * @return List of all Smarty 'function' plugins.
	 */
	public static Collection<String> getSmartyFunctionPluginNames (Project project, String prefix)
	{
		return getSmartyPluginNamesOfType(project, prefix, "function");
	}

	/**
	 * Get a list of all the Smarty 'modifier' plugins in the current project.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 *
	 * @return List of all Smarty 'modifier' plugins.
	 */
	public static Collection<String> getSmartyModifierPluginNames (Project project, String prefix)
	{
		return getSmartyPluginNamesOfType(project, prefix, "modifier");
	}

	/**
	 * Get a list of all the Smarty plugins of a given type in the current project. Strips off the "smarty_*_" part.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 * @param type    The type of Smarty plugin we're looking for.
	 *
	 * @return List of all Smarty plugins of a given type.
	 */
	protected static Collection<String> getSmartyPluginNamesOfType (Project project, String prefix, String type)
	{
		String smarty_prefix = "smarty_" + type + "_";

		Collection<String> plugins = PhpIndex.getInstance(project).getAllFunctionNames(
			new PlainPrefixMatcher(smarty_prefix + prefix)
			{
				public boolean prefixMatches (@NotNull String name)
				{
					return StringUtil.startsWithIgnoreCase(name, getPrefix());
				}
			}
		);

		// Strip off "smarty_*_" before we return the list.
		Collection<String> stripped_plugins = new ArrayList<String>();

		for (String plugin : plugins)
		{
			stripped_plugins.add(plugin.substring(smarty_prefix.length()));
		}

		return stripped_plugins;
	}

	/**
	 * Get a list of all the variables defined in a given Smarty file.
	 *
	 * @param editor The current editor.
	 * @param file   The file to look in.
	 *
	 * @return List of all defined variables.
	 */
	public static Collection<String> getSmartyVariables (Editor editor, PsiFile file)
	{
		Collection<String> variables = new ArrayList<String>();
		String variable;

		PsiElement[] root_elements = file.getChildren();

		// Look for tags (there are no nested tags - all tags are currently a child of a Smarty file).
		for (PsiElement root_element : root_elements)
		{
			if (!(root_element instanceof SmartyTag))
			{
				continue;
			}

			// Look for dollars.
			for (ASTNode dollar : root_element.getNode().getChildren(TokenSet.create(SmartyTokenTypes.DOLLAR)))
			{
				variable = getVariableName(dollar.getPsi().getNextSibling().getText());

				if (variable.length() > 0 && !variables.contains(variable))
				{
					boolean is_being_typed = false;

					for (Caret caret : editor.getCaretModel().getAllCarets())
					{
						PsiElement element_at_caret = file.findElementAt(caret.getOffset() - 1);

						if (element_at_caret != null && variable.equals(element_at_caret.getText()))
						{
							is_being_typed = true;
						}
					}

					if (!is_being_typed)
					{
						variables.add(variable);
					}
				}
			}

			// Look for assignments.
			ASTNode first_leaf = root_element.getNode().findLeafElementAt(1);

			if (first_leaf == null
				|| first_leaf.getElementType() != SmartyTokenTypes.PREDEFINED_FUNCTION
				|| !first_leaf.getText().equals("assign"))
			{
				continue;
			}

			// We've found an assignment. Look for the first attribute (which contains the variable name).
			PsiElement[] attributes = root_element.getChildren();

			if (attributes.length == 0 || !(attributes[0] instanceof SmartyAttribute))
			{
				continue;
			}

			// We've found an attribute.
			PsiElement attribute = attributes[0];

			// '{assign "variable_name" value}' notation. TODO: support expressions as well.
			ASTNode variable_node = attribute.getNode().findChildByType(SmartyTokenTypes.STRING_LITERAL);

			if (variable_node != null)
			{
				variable = getVariableName(variable_node.getText());

				if (variable.length() > 0 && !variables.contains(variable))
				{
					variables.add(variable);
				}

				continue;
			}

			// '{assign name="variable_name" value=value}' notation.
			PsiElement[] attribute_children = attribute.getChildren();

			if (attribute_children.length == 1)
			{
				ASTNode attribute_value = attribute_children[0].getNode();

				if (attribute_value.getElementType() == SmartyCompositeElementTypes.ATTRIBUTE_VALUE)
				{
					variable_node = attribute_value.findChildByType(SmartyTokenTypes.STRING_LITERAL);

					if (variable_node != null)
					{
						variable = variable_node.getText().trim();

						if (variable.length() > 0 && !variables.contains(variable))
						{
							variables.add(variable);
						}
					}
				}
			}
		}

		return variables;
	}

    /**
     * See if a given string is or starts with a valid variable. Return an empty string otherwise.
     *
     * @param potential_variable The string to look in.
     *
     * @return The variable, or an empty string if there is none.
     */
    protected static String getVariableName (String potential_variable)
    {
        // Strip everything from the first disallowed character.
        String variable = potential_variable.trim().replaceFirst("[^a-zA-Z0-9_].*$", "");

        // First character can't be a number.
        if (!variable.substring(0, 1).matches("[a-zA-Z_]"))
        {
            return "";
        }

        return variable;
    }

	/**
	 * Get a list of all the regular PHP functions (excluding Smarty functions) in the current project.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 *
	 * @return List of all available PHP functions.
	 */
	public static Collection<Function> getPHPFunctions (Project project, String prefix)
	{
		// Collect all PHP functions, except for smarty_* functions.
		Collection<String> function_names = PhpIndex.getInstance(project).getAllFunctionNames(
			new PlainPrefixMatcher(prefix)
			{
				public boolean prefixMatches (@NotNull String name)
				{
					return (
						!StringUtil.startsWithIgnoreCase(name, "smarty_") // Ignore smarty_* functions.
						&& StringUtil.containsIgnoreCase(name, getPrefix())
					);
				}
			}
		);

		Collection<Function> php_functions = new ArrayList<Function>();

		for (String function_name : function_names)
		{
			Function php_function = getCustomPHPFunctionByName(project, function_name);

			if (php_function != null)
			{
				php_functions.add(php_function);
			}
		}

		return php_functions;
	}

	/**
	 * Get a list of all the PHP classes in the current project.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 *
	 * @return List of all available PHP classes.
	 */
	public static Collection<PhpClass> getPHPClasses (Project project, String prefix)
	{
		Collection<String> class_names = PhpIndex.getInstance(project).getAllClassNames(new PlainPrefixMatcher(prefix));

		Collection<PhpClass> php_classes = new ArrayList<PhpClass>();

		for (String class_name : class_names)
		{
			PhpClass php_class = getPHPClassByName(project, class_name);

			if (php_class != null)
			{
				php_classes.add(php_class);
			}
		}

		return php_classes;
	}

	/**
	 * Get a list of all the PHP constants in the current project.
	 *
	 * @param project The project to look in.
	 * @param prefix  Optionally filter the list by a prefix.
	 *
	 * @return List of all available PHP constants.
	 */
	public static Collection<Constant> getPHPConstants (Project project, String prefix)
	{
		Collection<String> constant_names = PhpIndex.getInstance(project).getAllConstantNames(new PlainPrefixMatcher(prefix));

		Collection<Constant> php_constants = new ArrayList<Constant>();

		for (String constant_name : constant_names)
		{
			Constant php_constant = getPHPConstantByName(project, constant_name);

			if (php_constant != null)
			{
				php_constants.add(php_constant);
			}
		}

		return php_constants;
	}

	/**
	 * Get a PHP function by name. Return the first one we can find if there's more than one.
	 *
	 * @param project       The project to look in.
	 * @param function_name The full function name to look for.
	 *
	 * @return The PHP function object, or NULL.
	 */
	@Nullable
	public static Function getPHPFunctionByName (Project project, String function_name)
	{
		Collection<Function> matching_functions = PhpIndex.getInstance(project).getFunctionsByName(function_name);

		if (matching_functions.size() > 0)
		{
			// Just grab the first function.
			return (Function) matching_functions.toArray()[0];
		}

		return null;
	}

	/**
	 * Get a custom (non-API) PHP function by name. Return the first one we can find if there's more than one.
	 *
	 * @param project       The project to look in.
	 * @param function_name The full function name to look for.
	 *
	 * @return The PHP function object, or NULL.
	 */
	@Nullable
	public static Function getCustomPHPFunctionByName (Project project, String function_name)
	{
		Function php_function = getPHPFunctionByName(project, function_name);

		// Make sure we aren't listing any PHP API functions.
		if (php_function != null && php_function.getContainingFile().isWritable())
		{
			return php_function;
		}

		return null;
	}

	/**
	 * Get a PHP class by name.
	 *
	 * @param project    The project to look in.
	 * @param class_name The class name to look for.
	 *
	 * @return The PHP class object, or NULL.
	 */
	@Nullable
	public static PhpClass getPHPClassByName (Project project, String class_name)
	{
		return PhpIndex.getInstance(project).getClassByName(class_name);
	}

	/**
	 * Get the constants of a given PhpClass object.
	 *
	 * @param php_class The PhpClass object to look in.
	 *
	 * @return A collection of Field objects.
	 */
	public static Collection<Field> getConstantsOfPHPClass (PhpClass php_class)
	{
		Collection<Field> constants = new ArrayList<Field>();

		for (Field field : php_class.getFields())
		{
			if (field.isConstant())
			{
				constants.add(field);
			}
		}

		return constants;
	}

	/**
	 * Get the static methods of a given PhpClass object.
	 *
	 * @param php_class The PhpClass object to look in.
	 *
	 * @return A collection of Method objects.
	 */
	public static Collection<Method> getStaticMethodsOfPHPClass (PhpClass php_class)
	{
		Collection<Method> static_methods = new ArrayList<Method>();

		for (Method method : php_class.getMethods())
		{
			if (method.isStatic())
			{
				static_methods.add(method);
			}
		}

		return static_methods;
	}

	/**
	 * Get a PHP constant by name. Return the first one we can find if there's more than one.
	 *
	 * @param project       The project to look in.
	 * @param constant_name The constant name to look for.
	 *
	 * @return The PHP constant object, or NULL.
	 */
	@Nullable
	public static Constant getPHPConstantByName (Project project, String constant_name)
	{
		Collection<Constant> matching_constants = PhpIndex.getInstance(project).getConstantsByName(constant_name);

		if (matching_constants.size() > 0)
		{
			// Just grab the first function.
			return (Constant) matching_constants.toArray()[0];
		}

		return null;
	}
}
