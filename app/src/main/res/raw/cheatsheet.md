# Markdown Cheatsheet

A quick reference for the Markdown syntax supported by ReadMD.

## Text

Markdown uses asterisks and backticks for inline styling:

- **Bold text** — `**bold**`
- *Italic text* — `*italic*`
- ***Bold and italic*** — `***both***`
- ~~Strikethrough~~ — `~~text~~`
- `Inline code` — `` `code` ``
- [A link](https://commonmark.org) — `[text](url)`
- Subscript~not supported~ and superscript^not supported^

## Headings

Six levels, from `# H1` to `###### H6`. This document uses them —
open the table of contents to navigate.

### Level 3

#### Level 4

## Lists

### Unordered

- Apples
- Pears
  - Nested items work too
- Plums

### Ordered

1. First
2. Second
3. Third

### Task list

- [x] Done
- [ ] Not done yet
- [ ] Also not done

## Blockquotes

> The only way to do great work is to love what you do.
>
> — Steve Jobs

Quotes can span multiple paragraphs.

## Code

Inline code uses single backticks: `git status`.

Fenced blocks use triple backticks, with an optional language tag:

```python
def fibonacci(n: int) -> int:
    a, b = 0, 1
    for _ in range(n):
        a, b = b, a + b
    return a

print([fibonacci(i) for i in range(10)])
```

```
No language tag means plain monospace text.
```

## Tables

| Left aligned | Center aligned | Right aligned |
| :--- | :---: | ---: |
| a | b | c |
| longer cell | x | 42 |

## Horizontal rules

Three hyphens create a horizontal rule:

---

## Images

Images inline with `![alt text](url)` are supported for links the app
can resolve, including images stored next to your file in a picked folder.

That's it — everything you need to read (and write) Markdown.
