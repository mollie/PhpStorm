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

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import org.jetbrains.annotations.NotNull;

/**
 * This component replaces the active TypedHandler with a custom handler.
 */
public class SmartyTypedActionHandlerComponent implements ApplicationComponent
{
	protected SmartyTypedActionHandler typed_handler;

	/**
	 * Unused.
	 *
	 * @return An empty string.
	 */
	@NotNull
	public String getComponentName ()
	{
		return "";
	}

	/**
	 * Replace the default key handling.
	 */
	public void initComponent ()
	{
		EditorActionManager manager = EditorActionManager.getInstance();
		TypedAction action          = manager.getTypedAction();

		typed_handler = new SmartyTypedActionHandler(action.getHandler());
		action.setupHandler(typed_handler);
	}

	/**
	 * Restore the default key handling.
	 */
	public void disposeComponent ()
	{
		EditorActionManager manager = EditorActionManager.getInstance();
		TypedAction action          = manager.getTypedAction();

		action.setupHandler(typed_handler.getOriginalTypedHandler());
	}
}
