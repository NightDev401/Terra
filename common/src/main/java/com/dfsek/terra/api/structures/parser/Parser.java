package com.dfsek.terra.api.structures.parser;

import com.dfsek.terra.api.structures.parser.exceptions.ParseException;
import com.dfsek.terra.api.structures.parser.lang.Block;
import com.dfsek.terra.api.structures.parser.lang.Item;
import com.dfsek.terra.api.structures.parser.lang.Keyword;
import com.dfsek.terra.api.structures.parser.lang.Returnable;
import com.dfsek.terra.api.structures.parser.lang.constants.BooleanConstant;
import com.dfsek.terra.api.structures.parser.lang.constants.NumericConstant;
import com.dfsek.terra.api.structures.parser.lang.constants.StringConstant;
import com.dfsek.terra.api.structures.parser.lang.functions.Function;
import com.dfsek.terra.api.structures.parser.lang.functions.FunctionBuilder;
import com.dfsek.terra.api.structures.parser.lang.keywords.IfKeyword;
import com.dfsek.terra.api.structures.parser.lang.operations.BinaryOperation;
import com.dfsek.terra.api.structures.parser.lang.operations.BooleanNotOperation;
import com.dfsek.terra.api.structures.parser.lang.operations.ConcatenationOperation;
import com.dfsek.terra.api.structures.parser.lang.operations.NumberAdditionOperation;
import com.dfsek.terra.api.structures.tokenizer.Position;
import com.dfsek.terra.api.structures.tokenizer.Token;
import com.dfsek.terra.api.structures.tokenizer.Tokenizer;
import com.dfsek.terra.api.structures.tokenizer.exceptions.TokenizerException;
import com.dfsek.terra.api.util.GlueList;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Parser {
    private final String data;
    private final Map<String, FunctionBuilder<? extends Function<?>>> functions = new HashMap<>();
    private final Set<String> keywords = Sets.newHashSet("if");

    Set<Token.Type> allowedArguments = Sets.newHashSet(Token.Type.STRING, Token.Type.NUMBER, Token.Type.IDENTIFIER);

    public Parser(String data) {
        this.data = data;
    }

    public Parser addFunction(String name, FunctionBuilder<? extends Function<?>> functionBuilder) {
        functions.put(name, functionBuilder);
        return this;
    }

    public Block parse() throws ParseException {
        Tokenizer tokenizer = new Tokenizer(data);

        List<Token> tokens = new GlueList<>();
        try {
            while(tokenizer.hasNext()) tokens.add(tokenizer.fetch());
        } catch(TokenizerException e) {
            throw new ParseException("Failed to tokenize input", e);
        }
        // Check for dangling brackets
        int blockLevel = 0;
        for(Token t : tokens) {
            if(t.getType().equals(Token.Type.BLOCK_BEGIN)) blockLevel++;
            else if(t.getType().equals(Token.Type.BLOCK_END)) blockLevel--;
            if(blockLevel < 0) throw new ParseException("Dangling closing brace: " + t.getPosition());
        }
        if(blockLevel != 0) throw new ParseException("Dangling opening brace");

        return parseBlock(tokens);
    }


    @SuppressWarnings("unchecked")
    private Keyword<?> parseKeyword(List<Token> tokens) throws ParseException {

        Token identifier = tokens.remove(0);
        checkType(identifier, Token.Type.KEYWORD);
        if(!keywords.contains(identifier.getContent()))
            throw new ParseException("No such keyword " + identifier.getContent() + ": " + identifier.getPosition());
        Keyword<?> k = null;
        if(identifier.getContent().equals("if")) {

            checkType(tokens.remove(0), Token.Type.BODY_BEGIN);

            Returnable<?> comparator = parseExpression(tokens);
            checkReturnType(comparator, Returnable.ReturnType.BOOLEAN);

            checkType(tokens.remove(0), Token.Type.BODY_END);

            checkType(tokens.remove(0), Token.Type.BLOCK_BEGIN);

            k = new IfKeyword(parseBlock(tokens), (Returnable<Boolean>) comparator, identifier.getPosition());

        }
        return k;
    }

    @SuppressWarnings("unchecked")
    private Returnable<?> parseExpression(List<Token> tokens) throws ParseException {
        System.out.println(tokens.get(0));
        Token first = tokens.get(0);
        checkType(first, Token.Type.IDENTIFIER, Token.Type.BOOLEAN, Token.Type.STRING, Token.Type.NUMBER, Token.Type.BOOLEAN_NOT);

        boolean not = false;
        if(first.getType().equals(Token.Type.BOOLEAN_NOT)) {
            not = true;
            tokens.remove(0);
        }

        Returnable<?> expression;
        if(tokens.get(0).isConstant()) {
            Token constantToken = tokens.remove(0);
            Position position = constantToken.getPosition();
            switch(constantToken.getType()) {
                case NUMBER:
                    String content = constantToken.getContent();
                    expression = new NumericConstant(content.contains(".") ? Double.parseDouble(content) : Integer.parseInt(content), position);
                    break;
                case STRING:
                    expression = new StringConstant(constantToken.getContent(), position);
                    break;
                case BOOLEAN:
                    expression = new BooleanConstant(Boolean.parseBoolean(constantToken.getContent()), position);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported constant token: " + constantToken.getType() + " at position: " + position);
            }
        } else expression = parseFunction(tokens, false);


        if(not) {
            checkReturnType(expression, Returnable.ReturnType.BOOLEAN);
            expression = new BooleanNotOperation((Returnable<Boolean>) expression, expression.getPosition());
        }
        if(tokens.get(0).isBinaryOperator()) return parseBinaryOperation(expression, tokens);
        return expression;
    }

    @SuppressWarnings("unchecked")
    private BinaryOperation<?> parseBinaryOperation(Returnable<?> left, List<Token> tokens) throws ParseException {
        Token binaryOperator = tokens.remove(0);
        Returnable<?> right = parseExpression(tokens);

        switch(binaryOperator.getType()) {
            case ADDITION_OPERATOR:
                System.out.println(left.returnType());
                System.out.println(right.returnType());
                if(left.returnType().equals(Returnable.ReturnType.NUMBER) && right.returnType().equals(Returnable.ReturnType.NUMBER)) {
                    System.out.println("number " + binaryOperator.getPosition());
                    return new NumberAdditionOperation((Returnable<Number>) left, (Returnable<Number>) right, binaryOperator.getPosition());
                }
                return new ConcatenationOperation((Returnable<Object>) left, (Returnable<Object>) right, binaryOperator.getPosition());
            case BOOLEAN_OPERATOR:

            default:
                throw new UnsupportedOperationException("Unsupported binary operator: " + binaryOperator.getType());
        }

    }

    private Block parseBlock(List<Token> tokens) throws ParseException {
        List<Item<?>> parsedItems = new GlueList<>();
        Token first = tokens.get(0);

        checkType(tokens.get(0), Token.Type.IDENTIFIER, Token.Type.KEYWORD);
        main:
        while(tokens.size() > 0) {
            Token token = tokens.get(0);
            System.out.println(token);
            checkType(token, Token.Type.IDENTIFIER, Token.Type.KEYWORD, Token.Type.BLOCK_END);
            switch(token.getType()) {
                case KEYWORD:
                    parsedItems.add(parseKeyword(tokens));
                    if(tokens.isEmpty()) break;
                    checkType(tokens.get(0), Token.Type.IDENTIFIER, Token.Type.KEYWORD, Token.Type.BLOCK_END);
                    break;
                case IDENTIFIER:
                    parsedItems.add(parseFunction(tokens, true));
                    if(tokens.isEmpty()) break;
                    checkType(tokens.remove(0), Token.Type.STATEMENT_END, Token.Type.BLOCK_END);
                    break;
                case BLOCK_END:
                    tokens.remove(0);
                    break main;
            }
        }
        return new Block(parsedItems, first.getPosition());
    }

    private Function<?> parseFunction(List<Token> tokens, boolean fullStatement) throws ParseException {
        Token identifier = tokens.remove(0);
        checkType(identifier, Token.Type.IDENTIFIER); // First token must be identifier

        if(!functions.containsKey(identifier.getContent()))
            throw new ParseException("No such function " + identifier.getContent() + ": " + identifier.getPosition());

        checkType(tokens.remove(0), Token.Type.BODY_BEGIN); // Second is body begin


        List<Returnable<?>> args = getArgs(tokens); // Extract arguments, consume the rest.

        tokens.remove(0); // Remove body end

        if(fullStatement) checkType(tokens.get(0), Token.Type.STATEMENT_END);

        FunctionBuilder<?> builder = functions.get(identifier.getContent());
        if(args.size() != builder.getArguments() && builder.getArguments() != -1)
            throw new ParseException("Expected " + builder.getArguments() + " arguments, found " + args.size() + ": " + identifier.getPosition());
        return functions.get(identifier.getContent()).build(args, identifier.getPosition());
    }


    private List<Returnable<?>> getArgs(List<Token> tokens) throws ParseException {
        List<Returnable<?>> args = new GlueList<>();

        while(!tokens.get(0).getType().equals(Token.Type.BODY_END)) {
            args.add(parseExpression(tokens));
            checkType(tokens.get(0), Token.Type.SEPARATOR, Token.Type.BODY_END);
            if(tokens.get(0).getType().equals(Token.Type.SEPARATOR)) tokens.remove(0);
        }
        return args;
    }

    private void checkType(Token token, Token.Type... expected) throws ParseException {
        for(Token.Type type : expected) if(token.getType().equals(type)) return;
        throw new ParseException("Expected " + Arrays.toString(expected) + " but found " + token.getType() + ": " + token.getPosition());
    }

    private void checkReturnType(Returnable<?> returnable, Returnable.ReturnType... types) throws ParseException {
        for(Returnable.ReturnType type : types) if(returnable.returnType().equals(type)) return;
        throw new ParseException("Expected " + Arrays.toString(types) + " but found " + returnable.returnType() + ": " + returnable.getPosition());
    }
}