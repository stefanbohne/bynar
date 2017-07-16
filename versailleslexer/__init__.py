# -*- coding: utf-8 -*-

import re

from pygments.lexer import RegexLexer, bygroups, words
from pygments.token import Text, Comment, Operator, Keyword, Name, String, \
    Number, Punctuation

__all__ = ['VersaillesLexer']


class VersaillesLexer(RegexLexer):
    name = 'Versailles'
    filenames = ['*.vrs']
    aliases = ['vrs', 'versailles']
    mimetypes = ['text/x-vrssrc']

    flags = re.MULTILINE | re.UNICODE

    tokens = {
        'root': [
            (r'\n', Text),
            (r'\s+', Text),
            (r'\\\n', Text),  # line continuations
            (r'//(.*?)\n', Comment.Single),
            (r'/(\\\n)?[*](.|\n)*?[*](\\\n)?/', Comment.Multiline),
            (r'(import|module)\b', Keyword.Namespace),
            (r'(case|let|def|type|letdef|lettype|variant|forget|remember)\b',
             Keyword.Declaration),
            (words((
                'and', 'asserting', 'div', 'do', 'else', 
                'fail', 'for', 'from', 'if', 'iff', 'implies', 'in',  
                'loop', 'mod', 'or', 'pass', 'return', 'unless', 'until', 'where', 
                'yield'), suffix=r'\b'),
             Keyword),
            (r'(true|false|cons|nil|muladd|divmod|if|mod)\b', Name.Builtin),
            (r'(Number|String|Boolean|Type|Janus_class)\b', Name.Builtin),
            (words((
                'algebraic', 'inductive', 'tuple'), suffix=r'\b'),
             Keyword.Type),
            # float_lit
            (r'\d+(\.\d+[eE][+\-]?\d+|'
             r'\.\d*|[eE][+\-]?\d+)', Number.Float),
            (r'\.\d+([eE][+\-]?\d+)?', Number.Float),
            # int_lit
            # -- octal_lit
            (r'0[0-7]+', Number.Oct),
            # -- hex_lit
            (r'0[xX][0-9a-fA-F]+', Number.Hex),
            # -- decimal_lit
            (r'(0|[1-9][0-9]*)', Number.Integer),
            # StringLiteral
            (r'"[^"]*"', String),
            # -- interpreted_string_lit
            (r'\'(\\\\|\\\'|[^\'])*\'', String),
            # Tokens
            (r'(&&|\|\||==>|<=>|==|!=|<=|>=|<|>|'
             r'<>-<>|<>->|<-<>|>-<|>->|<-<|=>|->|>-|-->|[+\-*/!~])', Operator),
            (r'(::|[|!()\[\]\{\}\.,;:=\?])', Punctuation),
            # identifier
            (r'[^\W\d]\w*', Name.Other),
            (r'`[^`]*`', Name.Other),
        ]
    }