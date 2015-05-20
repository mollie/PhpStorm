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

package com.mollie.lang;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.smarty.lang.SmartyTokenTypes;
import com.jetbrains.smarty.lang.psi.SmartyCompositeElementTypes;
import com.mollie.icons.SmartyIcons;
import com.mollie.util.SmartyIndex;
import org.jetbrains.annotations.NotNull;

/**
 * Add Smarty function and generic PHP suggestions.
 */
public class SmartyCompletionContributor extends CompletionContributor
{
	protected static final Logger LOG = Logger.getInstance(SmartyCompletionContributor.class);

	/**
	 * Registers all of the CompletionProviders.
	 */
	public SmartyCompletionContributor ()
	{
		// Suggest Smarty block and function plugins when opening a Smarty tag.
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.START_TAG_START)),
			new CompletionProvider<CompletionParameters> ()
			{
				public void addCompletions(@NotNull CompletionParameters parameters,
				                           ProcessingContext context,
				                           @NotNull CompletionResultSet result_set)
				{
					String prefix   = result_set.getPrefixMatcher().getPrefix();
					Project project = parameters.getPosition().getProject();

					// Suggest block plugins.
					for (String name : SmartyIndex.getSmartyBlockPluginNames(project, prefix))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(name)
								.withTypeText("Smarty block")
								.withIcon(SmartyIcons.SmartyFile)
						);
					}

					// Suggest function plugins.
					for (String name : SmartyIndex.getSmartyFunctionPluginNames(project, prefix))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(name)
								.withTypeText("Smarty function")
								.withIcon(SmartyIcons.SmartyFile)
						);
					}
				}
			}
		);

		// Suggest PHP classes, PHP functions, PHP constants and Smarty variables where possible.
		extend(
			CompletionType.BASIC,
			PlatformPatterns.or( // TODO: this should be easier, and still doesn't work in all cases.
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.START_TAG_START)), // {
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.L_PAR)),           // (
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.L_BRACKET)),       // [
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.EQ)),              // =
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.GT)),              // >
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.LT)),              // <
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.EQ_EQ)),           // ==
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.GE)),              // >=
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.LE)),              // <=
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.NE)),              // !=
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.IDENTICAL)),       // ===
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.NOT_IDENTICAL)),   // !==
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.ADD)),             // +
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.SUB)),             // -
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.DIV)),             // /
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.MUL)),             // *
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.DOT)),             // .
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.COMMA)),           // ,
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.MOD)),             // %
				PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.ARRAY_ASSIGNMENT)) // =>
			),
			new CompletionProvider<CompletionParameters>()
			{
				public void addCompletions(@NotNull CompletionParameters parameters,
				                           ProcessingContext context,
				                           @NotNull CompletionResultSet result_set)
				{
					String prefix   = result_set.getPrefixMatcher().getPrefix();
					Project project = parameters.getPosition().getProject();

					// Suggest PHP functions.
					for (Function function : SmartyIndex.getPHPFunctions(project, prefix))
					{
						result_set.addElement(createLookupElementForFunction(function));
					}

					// Suggest PHP classes.
					for (PhpClass php_class : SmartyIndex.getPHPClasses(project, prefix))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(php_class.getName())
								.withIcon(php_class.getIcon())
								.withTypeText(php_class.getName())
								.withInsertHandler(createInsertHandler("::"))
						);
					}

					// Suggest PHP constants.
					for (Constant php_constant : SmartyIndex.getPHPConstants(project, prefix))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(php_constant.getName())
								.withIcon(php_constant.getIcon())
								.withTypeText(php_constant.getType().toStringResolved())
						);
					}

					// Suggest variables in the current Smarty file.
					for (String variable : SmartyIndex.getSmartyVariables(parameters.getEditor(), parameters.getOriginalFile()))
					{
						result_set.addElement(
							LookupElementBuilder
								.create("$" + variable)
								.withIcon(AllIcons.Nodes.Variable)
								.withTypeText("variable")
						);
					}
				}
			}
		);

		// Suggest Smarty variables when typing a dollar sign.
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.DOLLAR)),
			new CompletionProvider<CompletionParameters>()
			{
				public void addCompletions(@NotNull CompletionParameters parameters,
				                           ProcessingContext context,
				                           @NotNull CompletionResultSet result_set)
				{
					// Suggest variables in the current Smarty file.
					for (String variable : SmartyIndex.getSmartyVariables(parameters.getEditor(), parameters.getOriginalFile()))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(variable) // No prefix, since we already typed the $.
								.withTypeText("variable")
								.withIcon(AllIcons.Nodes.Variable)
						);
					}
				}
			}
		);

		// Suggest static class properties when typing a double colon.
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.COLON_COLON)),
			new CompletionProvider<CompletionParameters>()
			{
				public void addCompletions(@NotNull CompletionParameters parameters,
				                           ProcessingContext context,
				                           @NotNull CompletionResultSet result_set)
				{
					Project project            = parameters.getPosition().getProject();
					PsiElement current_element = parameters.getPosition();

					// Find double colon and class name.
					if (current_element.getPrevSibling() == null || current_element.getPrevSibling().getPrevSibling() == null)
					{
						return;
					}

					// Find class name reference.
					String class_name  = current_element.getPrevSibling().getPrevSibling().getText();
					PhpClass php_class = SmartyIndex.getPHPClassByName(project, class_name);

					if (php_class == null)
					{
						return;
					}

					// Suggest ::class.
					result_set.addElement(LookupElementBuilder.create("class"));

					// Suggest static class methods.
					for (Method method : SmartyIndex.getStaticMethodsOfPHPClass(php_class))
					{
						result_set.addElement(createLookupElementForFunction(method));
					}

					// Suggest class constants.
					for (Field field : SmartyIndex.getConstantsOfPHPClass(php_class))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(field.getName())
								.withIcon(field.getIcon())
								.withTypeText(field.getType().toStringResolved())
						);
					}
				}
			}
		);

		// Suggest Smarty modifier plugins when typing the modifier symbol "|".
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement()
				.withParent(PlatformPatterns.psiElement(SmartyCompositeElementTypes.MODIFIER))
				.afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.OR)),
			new CompletionProvider<CompletionParameters> ()
			{
				public void addCompletions(@NotNull CompletionParameters parameters,
				                           ProcessingContext context,
				                           @NotNull CompletionResultSet result_set)
				{
					String prefix   = result_set.getPrefixMatcher().getPrefix();
					Project project = parameters.getPosition().getProject();

					// Suggest modifier plugins.
					for (String name : SmartyIndex.getSmartyModifierPluginNames(project, prefix))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(name)
								.withTypeText("Smarty modifier")
								.withIcon(SmartyIcons.SmartyFile)
						);
					}
				}
			}
		);

		// Suggest Smarty block plugins when typing a closing tag.
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement(SmartyTokenTypes.END_TAG_START)),
			new CompletionProvider<CompletionParameters> ()
			{
				public void addCompletions(@NotNull CompletionParameters parameters,
				                           ProcessingContext context,
				                           @NotNull CompletionResultSet result_set)
				{
					String prefix   = result_set.getPrefixMatcher().getPrefix();
					Project project = parameters.getPosition().getProject();

					// Suggest block plugins.
					for (String name : SmartyIndex.getSmartyBlockPluginNames(project, prefix))
					{
						result_set.addElement(
							LookupElementBuilder
								.create(name)
								.withTypeText("Smarty block")
								.withIcon(SmartyIcons.SmartyFile)
						);
					}
				}
			}
		);
	}

	/**
	 * Insert a string before and after the caret.
	 *
	 * @param prepend The string to insert before the caret's position.
	 * @param append  The string to insert after the caret's position.
	 *
	 * @return An InsertHandler object.
	 */
	protected BasicInsertHandler<LookupElement> createInsertHandler (final String prepend, final String append)
	{
		return new BasicInsertHandler<LookupElement> ()
		{
			@Override
			public void handleInsert (InsertionContext context, LookupElement element)
			{
				PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), prepend);
				context.getDocument().insertString(context.getTailOffset(), append);
			}
		};
	}

	/**
	 * Insert a string before the caret.
	 *
	 * @param prepend The string to insert before the caret's position.
	 *
	 * @return An InsertHandler object.
	 */
	protected BasicInsertHandler<LookupElement> createInsertHandler (final String prepend)
	{
		return createInsertHandler(prepend, "");
	}

	/**
	 * Create a LookupElement for a PHP function or method. This automatically generates the parameter list (resource
	 * heavy).
	 *
	 * @param php_function The function to create a LookupElement for.
	 *
	 * @return A fancy LookupElement.
	 */
	protected LookupElement createLookupElementForFunction (Function php_function)
	{
		// Create the parameter string.
		String tail_text = "(";

		for (Parameter parameter : php_function.getParameters())
		{
			if (!tail_text.equals("("))
			{
				tail_text += ", ";
			}

			if (parameter.isOptional())
			{
				tail_text += "[";
			}

			if (parameter.isPassByRef())
			{
				tail_text += "&";
			}

			tail_text += parameter.getName();

			String declared_type = parameter.getDeclaredType().toStringResolved();

			if (declared_type.length() > 0)
			{
				tail_text += " : " + declared_type;
			}

			if (parameter.isOptional())
			{
				PsiElement default_value = parameter.getDefaultValue();

				if (default_value != null)
				{
					tail_text += " = " + default_value.getText();
				}

				tail_text += "]";
			}
		}

		tail_text += ")";

		return LookupElementBuilder
			.create(php_function.getName())
			.withIcon(php_function.getIcon())
			.withTailText(tail_text, true)
			.withTypeText(php_function.getType().toStringResolved())
			.withInsertHandler(createInsertHandler("(", ")"));
	}
}
