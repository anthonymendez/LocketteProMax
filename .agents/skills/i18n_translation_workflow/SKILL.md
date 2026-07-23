---
name: i18n_translation_workflow
description: Workflow and best practices for adding or updating localization messages across all language files (lang.yml and lang_*.yml), preserving Minecraft color formatting, variable expansion tokens, and Gradle YAML escaping.
---

# Localization & Translation Workflow

This skill outlines the process for maintaining and updating localized message keys in `LocketteProMax`.

## 1. Inventory of Language Files
All language resources reside in `src/main/resources/`:
- `lang.yml` (English default)
- `lang_de.yml` (German)
- `lang_es.yml` (Spanish)
- `lang_fr.yml` (French)
- `lang_hu.yml` (Hungarian)
- `lang_it.yml` (Italian)
- `lang_ja.yml` (Japanese)
- `lang_ko.yml` (Korean)
- `lang_pl.yml` (Polish)
- `lang_zh-cn.yml` (Chinese Simplified)
- `lang_zh-tw.yml` (Chinese Traditional)

## 2. Multi-File Translation Requirement
Whenever a message key is added or modified in `lang.yml`, update all 10 localized `lang_*.yml` files simultaneously to maintain translation parity across all supported server locales.

## 3. Formatting & Token Rules
- **Color & Style Codes**: Retain standard Bukkit color codes (`&6`, `&c`, `&a`, `&e`, `&f`, `&4`) matching the tone of the message (e.g. `&c` for error/warning, `&a` for success).
- **Gradle Expansion Tokens**: Preserve `${pluginName}` placeholders exactly so Gradle's `processResources` task expands them dynamically at build time.
- **YAML Linebreak Escaping**:
  - **Never use `\n` inside double-quoted YAML strings**. Gradle expands `\n` to a physical newline before SnakeYAML reads the file, breaking YAML syntax.
  - **Use `\\n` instead** for linebreaks so Gradle leaves `\\n` intact for runtime unescaping by Java (`replace("\\n", "\n")`).

## 4. Verification
After editing language files, always run:
```bash
./dev.sh test
```
This verifies that Gradle resource filtering completes without syntax errors and all automated tests pass cleanly.
