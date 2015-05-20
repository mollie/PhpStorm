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

package com.mollie.editor;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.smarty.SmartyFile;
import com.jetbrains.smarty.lang.SmartyTokenTypes;
import com.jetbrains.smarty.lang.psi.SmartyCompositeElementTypes;
import com.mollie.util.SmartyIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Adds autocomplete for Smarty blocks.
 */
public class SmartyTypedActionHandler implements TypedActionHandler
{
	protected static final Logger LOG = Logger.getInstance(SmartyTypedActionHandler.class);

	private final TypedActionHandler original_handler;

	/**
	 * Preserve the original TypedHandler.
	 *
	 * @param original_handler The original TypedHandler.
	 */
	public SmartyTypedActionHandler (TypedActionHandler original_handler)
	{
		this.original_handler = original_handler;
	}

	/**
	 * Called when the user types a character. Currently only used to autocomplete Smarty blocks.
	 *
	 * @param editor  The active editor.
	 * @param c       The character that the usertyped (but has yet to be added to the file).
	 * @param context The data context.
	 */
	@Override
	public void execute (@NotNull final Editor editor, final char c, @NotNull final DataContext context)
	{
		// Have the original handler process the keystroke first.
		getOriginalTypedHandler().execute(editor, c, context);

		Project project   = editor.getProject();
		Document document = editor.getDocument();

		// Make sure we can insert stuff.
		if (project == null || editor.isViewer() || !document.isWritable())
		{
			return;
		}

		// Retrieve the current file and caret position.
		PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);

		// Check if we're in a Smarty file, typing a closing accolade.
		if (!(file instanceof SmartyFile) || c != '}')
		{
			return;
		}

		// Check at every caret if we're closing a block element.
		List<Caret> carets = editor.getCaretModel().getAllCarets();

		// Make sure to invert the list of carets to prevent caret position issues when inserting new content.
		Collections.reverse(carets);

		for (Caret caret : carets)
		{
			int caret_position = caret.getOffset();

			if (caret_position < 1)
			{
				continue;
			}

			// Get the element under the caret. (Note we have to move one position back due to the new character.)
			PsiElement element = file.findElementAt(caret_position - 1);

			if (element == null)
			{
				continue;
			}

			// TODO: don't close again if we're already in a closing tag.

			// See if the accolade is now part of a tag.
			PsiElement tag_element = element.getParent();

			if (tag_element == null || tag_element.getNode().getElementType() != SmartyCompositeElementTypes.TAG)
			{
				continue;
			}

			// Attempt to get the tag element.
			PsiElement tag_id_element = tag_element.findElementAt(1);

			if (tag_id_element == null || tag_id_element.getNode().getElementType() != SmartyTokenTypes.IDENTIFIER)
			{
				continue;
			}

			String tag_name = tag_id_element.getText();

			// If we have a valid block plugin, autocomplete with a closing tag.
			Collection<String> block_plugins = SmartyIndex.getSmartyBlockPluginNames(project, tag_name);

			for (String block_plugin : block_plugins)
			{
				if (block_plugin.equals(tag_name))
				{
					document.insertString(caret_position, "{/" + tag_name + "}");
					break;
				}
			}
		}
	}

	/**
	 * Return the original TypedHandler. Allows dynamically disabling this type handler.
	 *
	 * @return The original TypedHandler.
	 */
	public TypedActionHandler getOriginalTypedHandler ()
	{
		return original_handler;
	}
}
