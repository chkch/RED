/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source;

import static com.google.common.collect.Lists.newArrayList;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createCommentRule;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createDefinitionRule;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createKeywordCallRule;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createKeywordUsageInSettings;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createLocalSettingRule;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createSectionHeaderRule;
import static org.robotframework.ide.eclipse.main.plugin.tableeditor.source.Rules.createVariableRule;

import java.util.List;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.CombinedAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.CycledContentAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.CycledContentAssistProcessor.AssitantCallbacks;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.GeneralSettingsAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.KeywordCallsAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.KeywordsInSettingsAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.LibrariesImportAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.ResourcesImportAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.SectionsAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.SettingsAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.SuiteSourceAssistantContext;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.VariablesAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.VariablesDefinitionsProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.VariablesImportAssistProcessor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.hyperlinks.HyperlinkToKeywordsDetector;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.hyperlinks.HyperlinkToVariablesDetector;
import org.robotframework.red.graphics.ColorsManager;

class SuiteSourceEditorConfiguration extends SourceViewerConfiguration {
    
    private final SuiteSourceEditor editor;

    public SuiteSourceEditorConfiguration(final SuiteSourceEditor editor) {
        this.editor = editor;
    }

    @Override
    public IAnnotationHover getAnnotationHover(final ISourceViewer sourceViewer) {
        return new DefaultAnnotationHover();
    }

    @Override
    public ITextHover getTextHover(final ISourceViewer sourceViewer, final String contentType) {
        return new SuiteSourceHoverSupport(editor.getFileModel());
    }

    @Override
    public IHyperlinkPresenter getHyperlinkPresenter(final ISourceViewer sourceViewer) {
        return super.getHyperlinkPresenter(sourceViewer);
    }

    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(final ISourceViewer sourceViewer) {
        final RobotSuiteFile model = editor.getFileModel();
        return new IHyperlinkDetector[] { new HyperlinkToVariablesDetector(model),
                new HyperlinkToKeywordsDetector(model) };
    }

    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(final ISourceViewer sourceViewer, final String contentType) {
        return new IAutoEditStrategy[] { new SuiteSourceIndentLineEditStrategy() };
    }

    @Override
    public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
        final ContentAssistant contentAssistant = new ContentAssistant();
        contentAssistant.enableColoredLabels(true);
        contentAssistant.enableAutoInsert(false);
        contentAssistant.enableAutoActivation(true);
        contentAssistant.setEmptyMessage("No proposals");
        contentAssistant.setShowEmptyList(true);
        contentAssistant.setStatusLineVisible(true);
        contentAssistant.setRepeatedInvocationMode(true);
        contentAssistant
                .setRepeatedInvocationTrigger(KeySequence.getInstance(KeyStroke.getInstance(SWT.CTRL, SWT.SPACE)));

        setupAssistantProcessors(contentAssistant);

        contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_BELOW);
        contentAssistant.setInformationControlCreator(new AbstractReusableInformationControlCreator() {

            @Override
            protected IInformationControl doCreateInformationControl(final Shell parent) {
                return new DefaultInformationControl(parent, true);
            }
        });
        return contentAssistant;
    }

    private void setupAssistantProcessors(final ContentAssistant contentAssistant) {
        final AssitantCallbacks assistantAccessor = new AssitantCallbacks() {

            @Override
            public void setStatus(final String title) {
                contentAssistant.setStatusMessage(title);
            }

            @Override
            public void openCompletionProposals() {
                contentAssistant.showPossibleCompletions();
            }
        };
        createSettingsAssist(contentAssistant, assistantAccessor);
        createVariablesAssist(contentAssistant, assistantAccessor);
        createKeywordsAssist(contentAssistant, assistantAccessor);
        createTestCasesAssist(contentAssistant, assistantAccessor);
        createDefaultAssist(contentAssistant, assistantAccessor);
    }

    private void createSettingsAssist(final ContentAssistant contentAssistant,
            final AssitantCallbacks assistantAccessor) {
        final SuiteSourceAssistantContext assistContext = new SuiteSourceAssistantContext(editor.getFileModel());

        final SectionsAssistProcessor sectionsAssistProcessor = new SectionsAssistProcessor(assistContext);
        final GeneralSettingsAssistProcessor settingNamesProcessor = new GeneralSettingsAssistProcessor(assistContext);
        final LibrariesImportAssistProcessor libraryImportsProcessor = new LibrariesImportAssistProcessor(assistContext);
        final VariablesImportAssistProcessor variableImportsProcessor = new VariablesImportAssistProcessor(
                assistContext);
        final ResourcesImportAssistProcessor resourceImportsProcessor = new ResourcesImportAssistProcessor(
                assistContext);
        final KeywordsInSettingsAssistProcessor keywordsAssistProcessor = new KeywordsInSettingsAssistProcessor(
                assistContext);
        final VariablesAssistProcessor variablesAssistProcessor = new VariablesAssistProcessor(assistContext);

        final CombinedAssistProcessor combinedProcessor = new CombinedAssistProcessor(libraryImportsProcessor,
                variableImportsProcessor, resourceImportsProcessor, sectionsAssistProcessor, settingNamesProcessor,
                keywordsAssistProcessor, variablesAssistProcessor);

        final CycledContentAssistProcessor cycledProcessor = new CycledContentAssistProcessor(assistContext,
                assistantAccessor);
        cycledProcessor.addProcessor(combinedProcessor);
        cycledProcessor.addProcessor(settingNamesProcessor);
        cycledProcessor.addProcessor(variablesAssistProcessor);

        contentAssistant.setContentAssistProcessor(cycledProcessor, SuiteSourcePartitionScanner.SETTINGS_SECTION);
        contentAssistant.addCompletionListener(cycledProcessor);
    }

    private void createVariablesAssist(final ContentAssistant contentAssistant,
            final AssitantCallbacks assistantAccessor) {
        final SuiteSourceAssistantContext assistContext = new SuiteSourceAssistantContext(editor.getFileModel());

        final SectionsAssistProcessor sectionsAssistProcessor = new SectionsAssistProcessor(assistContext);
        final VariablesDefinitionsProcessor variableDefsAssistProcessor = new VariablesDefinitionsProcessor(assistContext);
        final VariablesAssistProcessor variablesAssistProcessor = new VariablesAssistProcessor(assistContext);

        final CombinedAssistProcessor combinedProcessor = new CombinedAssistProcessor(variableDefsAssistProcessor,
                sectionsAssistProcessor, variablesAssistProcessor);

        final CycledContentAssistProcessor cycledProcessor = new CycledContentAssistProcessor(assistContext,
                assistantAccessor);
        cycledProcessor.addProcessor(combinedProcessor);
        cycledProcessor.addProcessor(variablesAssistProcessor);

        contentAssistant.setContentAssistProcessor(cycledProcessor, SuiteSourcePartitionScanner.VARIABLES_SECTION);
        contentAssistant.addCompletionListener(cycledProcessor);
    }

    private void createTestCasesAssist(final ContentAssistant contentAssistant,
            final AssitantCallbacks assistantAccessor) {
        final SuiteSourceAssistantContext assistContext = new SuiteSourceAssistantContext(editor.getFileModel());

        final SectionsAssistProcessor sectionsAssistProcessor = new SectionsAssistProcessor(assistContext);
        final KeywordCallsAssistProcessor keywordCallsAssistProcessor = new KeywordCallsAssistProcessor(assistContext);
        final SettingsAssistProcessor tcSettingsAssistProcessor = new SettingsAssistProcessor(assistContext);
        final VariablesAssistProcessor variablesAssistProcessor = new VariablesAssistProcessor(assistContext);

        final CombinedAssistProcessor combinedProcessor = new CombinedAssistProcessor(sectionsAssistProcessor,
                tcSettingsAssistProcessor, keywordCallsAssistProcessor, variablesAssistProcessor);

        final CycledContentAssistProcessor cycledProcessor = new CycledContentAssistProcessor(assistContext,
                assistantAccessor);
        cycledProcessor.addProcessor(combinedProcessor);
        cycledProcessor.addProcessor(keywordCallsAssistProcessor);
        cycledProcessor.addProcessor(variablesAssistProcessor);

        contentAssistant.setContentAssistProcessor(cycledProcessor, SuiteSourcePartitionScanner.TEST_CASES_SECTION);
        contentAssistant.addCompletionListener(cycledProcessor);
    }

    private void createKeywordsAssist(final ContentAssistant contentAssistant,
            final AssitantCallbacks assistantAccessor) {
        final SuiteSourceAssistantContext assistContext = new SuiteSourceAssistantContext(editor.getFileModel());

        final SectionsAssistProcessor sectionsAssistProcessor = new SectionsAssistProcessor(assistContext);
        final KeywordCallsAssistProcessor keywordCallsAssistProcessor = new KeywordCallsAssistProcessor(assistContext);
        final SettingsAssistProcessor kwSettingsAssistProcessor = new SettingsAssistProcessor(assistContext);
        final VariablesAssistProcessor variablesAssistProcessor = new VariablesAssistProcessor(assistContext);

        final CombinedAssistProcessor combinedProcessor = new CombinedAssistProcessor(sectionsAssistProcessor,
                kwSettingsAssistProcessor, keywordCallsAssistProcessor, variablesAssistProcessor);

        final CycledContentAssistProcessor cycledProcessor = new CycledContentAssistProcessor(assistContext,
                assistantAccessor);
        cycledProcessor.addProcessor(combinedProcessor);
        cycledProcessor.addProcessor(keywordCallsAssistProcessor);
        cycledProcessor.addProcessor(variablesAssistProcessor);

        contentAssistant.setContentAssistProcessor(cycledProcessor, SuiteSourcePartitionScanner.KEYWORDS_SECTION);
        contentAssistant.addCompletionListener(cycledProcessor);
    }

    private void createDefaultAssist(final ContentAssistant contentAssistant,
            final AssitantCallbacks assistantAccessor) {
        // we are adding all the assistants for default content type. Most of them (excluding
        // section headers assistant) are working in default content type only at the very last
        // position in file (this position always has default content type, but it can be actually
        // prepended with some valid meaningful content type

        final SuiteSourceAssistantContext assistContext = new SuiteSourceAssistantContext(editor.getFileModel());

        final SectionsAssistProcessor sectionsAssistProcessor = new SectionsAssistProcessor(assistContext);
        final GeneralSettingsAssistProcessor generalSettingNamesProcessor = new GeneralSettingsAssistProcessor(
                assistContext);
        final VariablesDefinitionsProcessor variableDefsAssistProcessor = new VariablesDefinitionsProcessor(
                assistContext);
        final LibrariesImportAssistProcessor libraryImportsProcessor = new LibrariesImportAssistProcessor(
                assistContext);
        final VariablesImportAssistProcessor variableImportsProcessor = new VariablesImportAssistProcessor(
                assistContext);
        final ResourcesImportAssistProcessor resourceImportsProcessor = new ResourcesImportAssistProcessor(
                assistContext);
        final KeywordsInSettingsAssistProcessor keywordsAssistProcessor = new KeywordsInSettingsAssistProcessor(
                assistContext);
        final SettingsAssistProcessor settingsAssistProcessor = new SettingsAssistProcessor(assistContext);
        final KeywordCallsAssistProcessor keywordCallsAssistProcessor = new KeywordCallsAssistProcessor(assistContext);
        final VariablesAssistProcessor variablesAssistProcessor = new VariablesAssistProcessor(assistContext);

        final CombinedAssistProcessor combinedProcessor = new CombinedAssistProcessor(variableDefsAssistProcessor,
                sectionsAssistProcessor, generalSettingNamesProcessor, libraryImportsProcessor,
                variableImportsProcessor, resourceImportsProcessor, keywordsAssistProcessor, settingsAssistProcessor,
                keywordCallsAssistProcessor, variablesAssistProcessor);

        final CycledContentAssistProcessor cycledProcessor = new CycledContentAssistProcessor(assistContext,
                assistantAccessor);
        cycledProcessor.addProcessor(combinedProcessor);

        contentAssistant.setContentAssistProcessor(cycledProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        contentAssistant.addCompletionListener(cycledProcessor);
    }

    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(final ISourceViewer sourceViewer) {
        final IQuickAssistAssistant assistant = new QuickAssistAssistant();
        assistant.setQuickAssistProcessor(new SuiteSourceQuickAssistProcessor(editor.getFileModel()));
        assistant.setInformationControlCreator(new AbstractReusableInformationControlCreator() {

            @Override
            protected IInformationControl doCreateInformationControl(final Shell parent) {
                return new DefaultInformationControl(parent, true);
            }
        });
        return assistant;
    }

    @Override
    public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
        final List<String> legal = newArrayList(SuiteSourcePartitionScanner.LEGAL_CONTENT_TYPES);
        legal.add(0, IDocument.DEFAULT_CONTENT_TYPE);
        return legal.toArray(new String[0]);
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
        final PresentationReconciler reconciler = new PresentationReconciler();

        final IToken section = new Token(new TextAttribute(ColorsManager.getColor(255, 0, 0)));
        final IToken comment = new Token(new TextAttribute(ColorsManager.getColor(192, 192, 192)));
        final IToken definition = new Token(new TextAttribute(ColorsManager.getColor(0, 0, 0), null, SWT.BOLD));
        final IToken variable = new Token(new TextAttribute(ColorsManager.getColor(0, 128, 0)));
        final IToken call = new Token(new TextAttribute(ColorsManager.getColor(0, 128, 192), null, SWT.BOLD));
        final IToken setting = new Token(new TextAttribute(ColorsManager.getColor(149, 0, 85)));
        
        final IRule[] defaultRules = new IRule[] {};
        createDamageRepairer(reconciler, IDocument.DEFAULT_CONTENT_TYPE, defaultRules);

        final IRule[] testCasesRules = new IRule[] { createVariableRule(variable), createSectionHeaderRule(section),
                createDefinitionRule(definition), createLocalSettingRule(setting), createKeywordCallRule(call),
                createCommentRule(comment) };
        createDamageRepairer(reconciler, SuiteSourcePartitionScanner.TEST_CASES_SECTION, testCasesRules);

        final IRule[] keywordsRules = new IRule[] { createVariableRule(variable), createSectionHeaderRule(section),
                createDefinitionRule(definition), createLocalSettingRule(setting), createKeywordCallRule(call),
                createCommentRule(comment) };
        createDamageRepairer(reconciler, SuiteSourcePartitionScanner.KEYWORDS_SECTION, keywordsRules);

        final IRule[] settingsRules = new IRule[] { createVariableRule(variable), createSectionHeaderRule(section),
                createDefinitionRule(setting), createKeywordUsageInSettings(call), createCommentRule(comment) };
        createDamageRepairer(reconciler, SuiteSourcePartitionScanner.SETTINGS_SECTION, settingsRules);

        final IRule[] variablesRules = new IRule[] { createVariableRule(variable), createSectionHeaderRule(section),
                createDefinitionRule(variable), createCommentRule(comment) };
        createDamageRepairer(reconciler, SuiteSourcePartitionScanner.VARIABLES_SECTION, variablesRules);

        return reconciler;
    }

    private static void createDamageRepairer(final PresentationReconciler reconciler, final String contentType,
            final IRule[] rules) {
        final DefaultDamagerRepairer damagerRepairer = new DefaultDamagerRepairer(new SingleTokenScanner(rules));
        reconciler.setDamager(damagerRepairer, contentType);
        reconciler.setRepairer(damagerRepairer, contentType);
    }

    @Override
    public IReconciler getReconciler(final ISourceViewer sourceViewer) {
        return new MonoReconciler(getReconcilingStrategy(), true);
    }

    private IReconcilingStrategy getReconcilingStrategy() {
        return new SuiteSourceReconcilingStrategy(editor);
    }

    @Override
    public IContentFormatter getContentFormatter(final ISourceViewer sourceViewer) {
        final MultiPassContentFormatter formatter = new MultiPassContentFormatter(
                getConfiguredDocumentPartitioning(sourceViewer), IDocument.DEFAULT_CONTENT_TYPE);
        formatter.setMasterStrategy(new SuiteSourceFormattingStrategy());
        return formatter;
    }

    private static class SingleTokenScanner extends BufferedRuleBasedScanner {

        public SingleTokenScanner(final IRule[] rules) {
            setRules(rules);
        }
    };
}
