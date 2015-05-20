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

package com.mollie.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpWorkaroundUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NonNls
public class PhpReplaceClassNameStringWithClassKeyword extends PsiElementBaseIntentionAction
{
	protected static final Logger LOG = Logger.getInstance(PhpReplaceClassNameStringWithClassKeyword.class);

	/**
	 * Get intent description.
	 *
	 * @return Description string.
	 */
	@NotNull
	public String getText ()
	{
		return "Convert to ::class";
	}

	/**
	 * Get intent family name.
	 *
	 * @return Family name.
	 */
	@NotNull
	public String getFamilyName ()
	{
		return getText();
	}

	/**
	 * Check if the intent is available at the current position.
	 *
	 * @param project Current project.
	 * @param editor  Current editor.
	 * @param element Element selected by the caret.
	 *
	 * @return True if we can use the intent, false otherwise.
	 */
	public boolean isAvailable (@NotNull Project project, Editor editor, @Nullable PsiElement element)
	{
		if (element == null || !PhpWorkaroundUtil.isIntentionAvailable(element) || element.getParent() == null)
		{
			return false;
		}

		ASTNode ast_node = element.getNode();

		if (ast_node == null)
		{
			return false;
		}

		IElementType element_type = ast_node.getElementType();

		// Must be either a double quoted or a single quoted string.
		if (element_type != PhpTokenTypes.STRING_LITERAL && element_type != PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE)
		{
			return false;
		}

		// Unquote the class string if needed.
		String class_name  = getUnquotedString(element.getText());
		PhpIndex php_index = PhpIndex.getInstance(project);

		// Look for both regular and namespaced class names.
		return (
			class_name.length() > 0
			&& (php_index.getClassByName(class_name) != null || php_index.getClassesByFQN(class_name).size() > 0)
		);
	}

	/**
	 * The intent is invoked. Do magic.
	 *
	 * @param project Current project.
	 * @param editor  Current editor.
	 * @param element Element selected by the caret.
	 */
	public void invoke (@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException
	{
		String class_name = getUnquotedString(element.getText());

		if (class_name == null)
		{
			return;
		}

		ClassConstantReference ref = PhpPsiElementFactory.createFromText(project, ClassConstantReference.class, class_name + "::class");
		PsiElement container = element.getParent();

		if (ref != null && container instanceof StringLiteralExpression)
		{
			// Replace the string container with the class constant reference.
			container.replace(ref);
		}
	}

	/**
	 * Unquote a quoted string.
	 *
	 * @param string The string to unquote.
	 *
	 * @return The unquoted string.
	 */
	protected String getUnquotedString (String string)
	{
		int string_length = string.length();

		if ((string.substring(0, 1).equals("'") && string.substring(string_length - 1).equals("'"))
			|| (string.substring(0, 1).equals("\"") && string.substring(string_length - 1).equals("\"")))
		{
			return string.substring(1, string_length - 1);
		}

		return string;
	}
}
