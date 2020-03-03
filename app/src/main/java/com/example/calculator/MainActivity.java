//*********************************************************
//
// Tex Bishop
// COSC 6362 - Mobile Software Programming
// Homework #3: Calculator
// Due: March 5, 2020
// Instructor: Dr. Mamta Yadav
//
//*********************************************************

package com.example.calculator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Locale;
import java.util.Stack;

//==========================================================================
// Have MainActivity implement TextToSpeech.OnInitListener so that calls
// to the text to speech can be caught anywhere within the activity.
//==========================================================================
public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener
{
    //==========================================================================
    // TextToSpeech textToSpeech  Our object used to activate text to speech.
    // double storedAnswer      Stores the answer of the last calculation.
    // double storedPrevious      Stores the answer of the previous calculation.
    // boolean error            Keeps track of the error state.  Is flipped to
    //                          true when the translation of the voice
    //                          recognition formula fails, then flipped back
    //                          to false at the beginning of each new input.
    //==========================================================================
    TextToSpeech textToSpeech;
    private double storedAnswer = Double.NaN;   // Initialize to NaN, to allow for assignment checks
    private double storedPrevious = Double.NaN;
    private boolean error = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //==========================================================================
        // Set all TextViews to auto scale with screen size, except the output/formula
        // view.  This sets the text size to scale to the size of the view, and that
        // wouldn't work for the formula view.  It would need to be broken up into
        // a separate text view per line to do this with it, and I don't want to do that.
        // It would hinder wrapping for long equations.
        //==========================================================================
        final TextView instructionText = findViewById(R.id.instructionText);
        final TextView previousTag = findViewById(R.id.previousTag);
        final TextView answerTag = findViewById(R.id.answerTag);
        final TextView previousText = findViewById(R.id.previousText);
        final TextView answerText = findViewById(R.id.answerText);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(instructionText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(previousTag, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(answerTag, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(previousText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(answerText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

        //==========================================================================
        // Initialize the TextToSpeech object to this activity
        //==========================================================================
        textToSpeech = new TextToSpeech(this, this);

        //==========================================================================
        // Create and set the intent for voice recognition.
        //==========================================================================
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);

        //==========================================================================
        // Create the onClickListener for the voice activation button.
        //==========================================================================
        final Button speechButton = findViewById(R.id.speechButton);
        speechButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onInit(int i)
    {
        //==========================================================================
        // Required to be here because of this activity implementing text to speech,
        // but I don't have use for it.
        //==========================================================================
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //==========================================================================
        // This function is automatically called each time the voice recognition
        // stops recording, and processes the result.
        //==========================================================================
        super.onActivityResult(requestCode, resultCode, data);
        error = false;  // Reset the error state to false

        if (requestCode == 1)
        {
            if (resultCode == RESULT_OK && data != null)
            {
                //==========================================================================
                // TextView output              The textview used to display output.
                // ArrayList<String> results    Stores the results gathered by the voice
                //                              recognition.  This returns multiple translations
                //                              that can be chosen from.  We are only going to
                //                              use the first one, rather than parsing all
                //                              of them.
                // String[] words               Used to store the results of the first
                //                              item from the voice recognition list.
                //==========================================================================
                final TextView output = findViewById(R.id.outputText);
                final TextView previousText = findViewById(R.id.previousText);
                final TextView answerText = findViewById(R.id.answerText);
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String[] words = results.get(0).split(" ");

                //==========================================================================
                // If the command contains "clear", then clear the output, and both answer
                // fields, then reset the stored answers.  Otherwise, build the output.
                //==========================================================================
                if (results.get(0).contains("clear"))
                {
                    output.setText("");
                    previousText.setText("");
                    answerText.setText("");
                    storedAnswer = Double.NaN;
                    storedPrevious = Double.NaN;
                }
                else
                {
                    //==========================================================================
                    // If the output isn't currently empty, start by bumping the previous
                    // equation up a line.
                    //==========================================================================
                    if (output.getText().toString().matches("") == false)
                    {
                        output.append("\n");
                    }

                    //==========================================================================
                    // Translate the voice recognition input into an infix equation.  If no
                    // errors occur, then also calculate the answer.
                    //==========================================================================
                    String formula = getFormula(words);
                    double answer = 0;
                    if (!error)
                        answer = getAnswer(formula);

                    //==========================================================================
                    // If there are no errors, set the value of the previous answer to the
                    // currently stored answer, set the currently stored answer to the new
                    // answer, and set the text of the text views.
                    // Else on an error, print out the results of the voice recognition instead
                    // of the formula, so that we can see what was read incorrectly, and print
                    // "Error" in the answer field.  Move the previous good answer to
                    //  storedPrevious, and set storedAnswer to NaN.
                    //==========================================================================
                    if (!error)
                    {
                        if (!Double.isNaN(storedAnswer))
                        {
                            previousText.setText(Double.toString(storedAnswer));
                            storedPrevious = storedAnswer;
                        }
                        storedAnswer = answer;
                        output.append(formula);
                        answerText.setText(Double.toString(storedAnswer));
                        textToSpeech.speak("The answer is " + storedAnswer, TextToSpeech.QUEUE_ADD, null, "0");
                    }
                    else
                    {
                        output.append(results.get(0));
                        answerText.setText("Error");
                        textToSpeech.speak("I'm sorry, I didn't understand that.", TextToSpeech.QUEUE_ADD, null, "0");
                        if (!Double.isNaN(storedAnswer))
                        {
                            previousText.setText(Double.toString(storedAnswer));
                            storedPrevious = storedAnswer;
                            storedAnswer = Double.NaN;
                        }
                    }
                }
            }
        }
    }

    //=================================================================================
    // Translate the data retrieved from voice recognition into an infix format formula
    // stored as a String.
    //
    // Return value
    // -----------------
    // String                       The resulting formula after translating.
    //
    // Parameters
    // -----------------
    // String[]       words         The String array containing the results of the
    //                              voice recognition.
    //
    //=================================================================================
    private String getFormula(String[] words)
    {
        //==========================================================================
        // StringBuilder formula    Used to build and store the translated infix
        //                          format version of the voice recognition formula.
        // double number            Used to store a converted number for calculation.
        //==========================================================================
        StringBuilder formula = new StringBuilder();
        double number;

        for (int i = 0; i < words.length && !error; i++)
        {
            if (NumberUtils.isCreatable(words[i]))
            {
                number = getDouble(words[i]);
                //=======================================================
                // The voice recognition will sometimes return numbers
                // with million/thousand/hundred in word form.  Resolve
                // that here.
                //=======================================================
                if (i + 1 < words.length)
                {
                    switch (words[i + 1])
                    {
                        case "million":
                            number *= 1000000;
                            break;
                        case "thousand":
                            number *= 1000;
                            break;
                        case "hundred":
                            number *= 100;
                            break;
                    }
                }
                formula.append(number);
                formula.append(" ");
            }
            else
            {
                //=======================================================
                // The voice recognition will sometimes return single
                // digit numbers as words.  Resolve that here.
                //=======================================================
                if (words[i].matches("zero|one|to|two|three|four|five|six|seven|eight|nine"))
                {
                    switch (words[i])
                    {
                        case "zero":
                            formula.append("0 ");
                            break;
                        case "one":
                            formula.append("1 ");
                            break;
                        case "to":
                        case "two":
                            formula.append("2 ");
                            break;
                        case "three":
                            formula.append("3 ");
                            break;
                        case "four":
                            formula.append("4 ");
                            break;
                        case "five":
                            formula.append("5 ");
                            break;
                        case "six":
                            formula.append("6 ");
                            break;
                        case "seven":
                            formula.append("7 ");
                            break;
                        case "eight":
                            formula.append("8 ");
                            break;
                        case "nine":
                            formula.append("9 ");
                            break;
                    }
                }
                else
                {
                    //=======================================================
                    // Handle the various translations of operands here.
                    //=======================================================
                    switch (words[i])
                    {
                        case "plus":
                        case "+":
                            formula.append("+ ");
                            break;
                        case "minus":
                        case "-":
                            formula.append("- ");
                            break;
                        case "multiplied":
                        case "times":
                        case "*":
                            formula.append("* ");
                            break;
                        case "divided":
                        case "/":
                            formula.append("/ ");
                            break;
                        case "squared":
                            formula.replace((formula.length() - 1), formula.length(), "");
                            formula.append("\u00B2 ");
                            break;
                        case "cubed":
                            formula.replace((formula.length() - 1), formula.length(), "");
                            formula.append("\u00B3 ");
                            break;
                        case "root":
                        case "√":
                            if (0 < i)
                            {
                                if (words[i - 1].matches("cube"))
                                    formula.append("∛");
                                else
                                    formula.append("√");
                            } else
                                formula.append("√");
                            break;
                        case "answer":
                            formula.append(storedAnswer);
                            formula.append(" ");
                            break;
                        case "previous":
                            formula.append(storedPrevious);
                            formula.append(" ");
                            break;
                        case "open":
                            formula.append("( ");
                            break;
                        case "close":
                        case "clothes": // Because voice recognition likes mistaking close as clothes
                            formula.append(") ");
                            break;
                        case "foreclose": // Common mis-translation
                        case "foreclosed":
                            formula.append("4 ) ");
                            break;
                        case "OnePlus": // Common mis-translation
                            formula.append("1 + ");
                            break;
                        //=======================================================
                        // Continue handling common mis-translation cases here.
                        //=======================================================
                    }
                }
            }
        }

        return formula.toString();
    }

    //=================================================================================
    // Determine the resulting answer of an infix formula stored as a String.  Resolve
    // squares/cubes and square root / cube root before converting the formula to
    // postfix format.
    //
    // Return value
    // -----------------
    // double                       The resulting answer after calculating.
    //
    // Parameters
    // -----------------
    // String       formula         The infix formula in String format.
    //
    //=================================================================================
    private double getAnswer(String formula)
    {
        //==========================================================================
        // double answer        Used to hold the calculated answer.
        // double number        Used to store a converted number for calculation.
        // String[] operands    An array holding the formula in infix format.
        //==========================================================================
        double answer;
        double number;
        String[] operands = formula.split(" ");

        for (int i = 0; i < operands.length && !error; i++)
        {
            //=======================================================
            // Handle square/cube and roots immediately, to simplify
            // the process of converting to postfix.
            //-------------------------------------------------------
            // Remove the operand from the String, then perform the
            // operation and store the result back into the String.
            //=======================================================
            if (operands[i].contains("√"))
            {
                operands[i] = operands[i].replace("√", "");
                number = getDouble(operands[i]);
                operands[i] = Math.sqrt(number) + "";
            }
            else if (operands[i].contains("∛"))
            {
                operands[i] = operands[i].replace("∛", "");
                number = getDouble(operands[i]);
                operands[i] = Math.cbrt(number) + "";
            }
            else if (operands[i].contains("\u00B2"))
            {
                operands[i] = operands[i].replace("\u00B2", "");
                number = getDouble(operands[i]);
                operands[i] = (number * number) + "";
            }
            else if (operands[i].contains("\u00B3"))
            {
                operands[i] = operands[i].replace("\u00B3", "");
                number = getDouble(operands[i]);
                operands[i] = (number * number * number) + "";
            }
        }

        //=======================================================
        // If the formula has unfinished operations, convert to
        // postfix and calculate the answer.  Else, make the single
        // remaining number the answer and exit.
        //=======================================================
        if (operands.length != 1)
        {
            Stack<String> postfix = stackify(operands);
            answer = calculate(postfix);
        }
        else
        {
            answer = getDouble(operands[0]);
        }


        return answer;
    }

    //=================================================================================
    // Determine the precedence of an operator.
    //
    // Return value
    // -----------------
    // int                          The precedence value.
    //
    // Parameters
    // -----------------
    // String       character       The operator being evaluated.
    //
    //=================================================================================
    private int precedence(final String character)
    {
        switch (character)
        {
            case "+":
            case "-":
                return 0;
            case "*":
            case "/":
                return 1;
            default:
                return 2;  // Parentheses
        }
    }

    //=================================================================================
    // Recursively move operators from the operator stack to the postfix formula stack,
    // until the currently evaluated operator is the lowest precedence.
    //
    // Return value
    // -----------------
    // None
    //
    // Parameters
    // -----------------
    // Stack<String>    opr             The operators that currently have yet to be
    //                                  placed in the postfix formula.
    // Stack<String>    postfix         The current postfix formula (mid-build).
    // String           character       The operator being evaluated for precedence.
    //
    //=================================================================================
    private void evalOperators(Stack<String> opr, Stack<String> postfix, final String character)
    {	//push and pop, then check to see if we need to push and pop again
        postfix.push(opr.peek());
        opr.pop();
        if (!opr.isEmpty() && opr.peek().equals("(")) //Don't do if operator stack is empty, or we're inside parentheses
            if (precedence(opr.peek()) >= precedence(character))
                evalOperators(opr, postfix, character); // Continue doing this recursively, until the character/operator has precedence
    }

    //=================================================================================
    // Take a formula stored in infix format in a String array, and convert it to a
    // postfix format and store it in a Stack.
    //
    // Return value
    // -----------------
    // Stack<String>                The resulting Stack the postfix formula is stored in.
    //
    // Parameters
    // -----------------
    // String[]       operands      The String array containing the infix format formula.
    //
    //=================================================================================
    private Stack<String> stackify(String[] operands)
    {
        //==========================================================================
        // Stack<String> postfix    The stack we will push our postfix formula into.
        // Stack<String> opr        The stack we will store operators in temporarily,
        //                          until they're ready to be placed in the postfix formula.
        // Stack<String> returnStack  The stack that will contain the completed formula.
        // String character         The current number/operator being evaluated
        //==========================================================================
        Stack<String> postfix = new Stack<>();
        Stack<String> opr = new Stack<>();
        Stack<String> returnStack = new Stack<>();
        String character;

        for (int i = 0; i < operands.length && !error; i++)
        {
            character = operands[i];
            switch (character)
            {
                case "(":
                case "+":
                case "-":
                case "*":
                case "/":
                    //=======================================================
                    // If the operator stack is empty, push the operation.
                    // Else, if the current operator has precedence, or is
                    // open parenthesis, push.  Else, call evalOperators to
                    // determine where to place the operator.
                    //=======================================================
                    if (opr.isEmpty())
                        opr.push(character);
                    else
                    {
                        if (precedence(opr.peek()) < precedence(character) || opr.peek().equals("("))
                            opr.push(character);
                        else
                        {
                            evalOperators(opr, postfix, character);
                            opr.push(character);
                        }
                    }
                    break;
                case ")":
                    //=======================================================
                    // When we reach close parentheses, begin pushing operators
                    // until we find close parentheses, then pop the open
                    // parentheses.
                    //=======================================================
                    while (!opr.peek().equals("("))
                    {
                        postfix.push(opr.peek());
                        opr.pop();
                    }
                    opr.pop();
                    break;
                default:
                    postfix.push(character); //Default push, called on anything not an operator (numbers)
                    break;
            }
        }

        //=======================================================
        // We've reached the end of the formula.  Push any
        // operators remaining onto the top of the stack.  Then
        // reverse the stack order into the return Stack, so that
        // it's in proper postfix order when popping.
        //=======================================================
        while (!opr.isEmpty())
        {
            postfix.push(opr.peek());
            opr.pop();
        }
        while (!postfix.isEmpty())
        {
            returnStack.push(postfix.peek());
            postfix.pop();
        }
        return returnStack;
    }

    //=================================================================================
    // Calculate the answer to a formula stored in postfix format.
    //
    // Return value
    // -----------------
    // double                       The resulting answer.
    //
    // Parameters
    // -----------------
    // Stack<String>    postfix     A stack containing the postfix formula.
    //
    //=================================================================================
    private double calculate(Stack<String> postfix)
    {
        //==========================================================================
        // Stack<Double> numbers    Used to hold the numbers as we work through the
        //                          postfix calculations.
        // double first             Holds the first number for a calculation.
        // double second            Holds the second number for a calculation.
        //==========================================================================
        Stack<Double> numbers = new Stack<>();
        double first = 1, second;

        while (!postfix.isEmpty() && !error)
        {
            //=======================================================
            // If the top of the stack is a number, push it onto the
            // number stack and pop from the postfix formula.
            // Else, it's an operator.  Pop the top two numbers from
            // the number stack and perform the calculation.  Push
            // the resulting answer onto the number stack.
            //=======================================================
            if (NumberUtils.isCreatable(postfix.peek()))
            {
                numbers.push(getDouble(postfix.peek()));
                postfix.pop();
            }
            else
            {
                try
                {
                    second = numbers.pop();
                    first = numbers.pop();
                }
                catch (EmptyStackException e)
                {
                    error = true;   // Flag the translation as failed
                    break;
                }

                switch(postfix.peek())
                {
                    case "+":
                        numbers.push(first + second);
                        break;
                    case "-":
                        numbers.push(first - second);
                        break;
                    case "*":
                        numbers.push(first * second);
                        break;
                    case "/":
                        numbers.push(first / second);
                        break;
                }
                postfix.pop();
            }
        }

        //=======================================================
        // The postfix formula stack is empty.  The answer should
        // be the single remaining number in the numbers stack.
        //=======================================================
        try
        {
            first = numbers.pop();
        }
        catch (EmptyStackException e)
        {
            error = true;   // Flag the translation as failed
        }
        return first;
    }

    //=================================================================================
    // Attempt to convert a String variable into a double.  Set error state on failure.
    //
    // Return value
    // -----------------
    // double                       The resulting number after converting.
    //
    // Parameters
    // -----------------
    // String       strNumber       The string to try and convert.
    //
    //=================================================================================
    private double getDouble(String strNumber)
    {
        try
        {
            return NumberUtils.createDouble(strNumber);
        }
        catch (NumberFormatException e)
        {
            error = true;   // Flag the translation as failed
            return 1;
        }
    }
}
