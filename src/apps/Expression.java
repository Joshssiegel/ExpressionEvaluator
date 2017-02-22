package apps;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;                

	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   

	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;

	/**
	 * String containing all delimiters (characters other than variables and constants), 
	 * to be used with StringTokenizer
	 */
	public static final String delims = " \t*+-/()[]";

	/**
	 * Initializes this Expression object with an input expression. Sets all other
	 * fields to null.
	 * 
	 * @param expr Expression
	 */
	public Expression(String expr) {
		this.expr = expr;
	}

	/**
	 * Populates the scalars and arrays lists with symbols for scalar and array
	 * variables in the expression. For every variable, a SINGLE symbol is created and stored,
	 * even if it appears more than once in the expression.
	 * At this time, values for all variables are set to
	 * zero - they will be loaded from a file in the loadSymbolValues method.
	 */
	public void buildSymbols() {
		/** COMPLETE THIS METHOD **/
		scalars=new ArrayList<>();
		arrays=new ArrayList<>();
		int index=0;
		String symbol="";
		char[] tokens=expr.toCharArray();
		for(int i=0;i<expr.length();i++)
		{
			while(i<tokens.length&& ( (tokens[i]>='A'&&tokens[i]<='Z') || (tokens[i]>='a'&&tokens[i]<='z')))
			{
				symbol=symbol.concat(""+tokens[i]);
				if(i+1<tokens.length)
					i++;
				else
					break;
			}

			if(tokens[i]=='['&&symbol!="")
			{
				ArraySymbol arrVariable=new ArraySymbol(symbol);
				if(!arrays.contains(arrVariable))
				{
					arrays.add(arrVariable);
					System.out.println("New Array Symbol: "+symbol);
				}

			}
			else if(symbol!="")
			{

				ScalarSymbol scalarVariable=new ScalarSymbol(symbol);
				if(!scalars.contains(scalarVariable))
				{
					scalars.add(scalarVariable);
					System.out.println("New Scalar Symbol: "+symbol);
				}

			}

			symbol="";
		}
	}

	/**
	 * Loads values for symbols in the expression
	 * 
	 * @param sc Scanner for values input
	 * @throws IOException If there is a problem with the input 
	 */
	public void loadSymbolValues(Scanner sc) 
			throws IOException {
		while (sc.hasNextLine()) {
			StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
			int numTokens = st.countTokens();
			String sym = st.nextToken();
			ScalarSymbol ssymbol = new ScalarSymbol(sym);
			ArraySymbol asymbol = new ArraySymbol(sym);
			int ssi = scalars.indexOf(ssymbol);
			int asi = arrays.indexOf(asymbol);
			if (ssi == -1 && asi == -1) {
				continue;
			}
			int num = Integer.parseInt(st.nextToken());
			if (numTokens == 2) { // scalar symbol
				scalars.get(ssi).value = num;
			} else { // array symbol
				asymbol = arrays.get(asi);
				asymbol.values = new int[num];
				// following are (index,val) pairs
				while (st.hasMoreTokens()) {
					String tok = st.nextToken();
					StringTokenizer stt = new StringTokenizer(tok," (,)");
					int index = Integer.parseInt(stt.nextToken());
					int val = Integer.parseInt(stt.nextToken());
					asymbol.values[index] = val;              
				}
			}
		}
	}


	/**
	 * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
	 * subscript expressions.
	 * 
	 * @return Result of evaluation
	 */
	public float evaluate() {
		/** COMPLETE THIS METHOD **/
		//Get Rid of Spaces
		expr=expr.trim();
		while(expr.contains(" ")){
			for(int i=0;i<expr.length();i++)
			{
				if(expr.charAt(i)==' ')
				{
					expr=expr.substring(0, i)+expr.substring(i+1);
				}
			}
		}
		
		
		System.out.println("Starting: "+expr);
		System.out.println("Arrays: ");
		printArrays();
		System.out.println("Scalars:");
		printScalars();


		return evaluateRecursively(expr);
	}

	private float evaluateRecursively(String expression)
	{
		
		
		System.out.println("Starting expression: "+expression);
		expression=replaceVariablesWithScalar(expression);
		System.out.println("Substituted Variables: "+expression);
		Stack<Character> stack=new Stack<Character>();
		int low=0,hi=expression.length()-1;
		while(expression.contains("("))
		{
			low=-1; hi=expression.length()-1;
			stack.clear();
			for(int i=0;i<expression.length();i++)
			{
				char c=expression.charAt(i);
				if(c=='('||c=='[')
				{
					if(low==-1)//Please Need to Check case where there are two equally place parens, i.e. (x+1)*(x-1)
						low=i;
					if(c=='(')
						stack.push(')');
					else
						stack.push(']');
				}
				else if(c==')'||c==']')
				{
					if(c==stack.pop()&&stack.isEmpty())
					{
						hi=i;
						//	Should be replace part of expression between low and hi with this
						expression=expression.substring(0,low)+evaluateRecursively(expression.substring(low+1, hi))+expression.substring(hi+1, expression.length());

						System.out.println("New Expression: "+expression);
						break;
					}
				}
			}
		}


		//Evaluate * and /
		Stack<Float> values=new Stack<Float>();
		Stack<Character> operators=new Stack<Character>();

		//Search for multiplications and divisions, do them first.
		//Then search for additions and subtractions and do them. then return that value
		while(expression.contains("*")||expression.contains("/"))
		{
			low=0;hi=expression.length()-1;
			for(int i=0;i<expression.length();i++)
			{
				String strVal="";


				if(expression.charAt(i)=='*')
				{
					//Find the value to the right of the operator
					values.push(findFloatRightOfOperator(i, expression));

					System.out.println("MULTIPLYING ON RIGHT: "+values.peek());

					//Find value to the left of the operator		
					values.push(findFloatToLeftOfOperator(i, expression));
					System.out.println("MULTIPLYING ON LEFT: "+values.peek());


					//Do the operation and put it on the stack
					values.push(values.pop()*values.pop());//Left value*Right Value
					low=findLowIndex(i, expression);
					hi=findHighIndex(i, expression);


					if(hi!=expression.length()-1)
					{
						if(!operators.isEmpty())
							expression=expression.substring(0,low)+operators.pop()+values.peek()+expression.substring(hi, expression.length());
						else
						{
							System.out.print("Replacing: "+expression+" with: ");
							expression=expression.substring(0,low)+values.peek()+expression.substring(hi, expression.length());	
							System.out.println(expression);
						}
					}
					else
					{
						if(!operators.isEmpty())
							expression=expression.substring(0,low)+operators.pop()+values.peek();
						else
							expression=""+values.peek();


					}
					System.out.println("After Multiplication symplifying: "+expression);

					i=0;
					break;
				}
				else if(expression.charAt(i)=='/')
				{
					//Find the value to the right of the operator

					values.push(findFloatRightOfOperator(i, expression));
					System.out.println("Dividing ON RIGHT: "+values.peek());

					//Find value to the left of the operator		
					values.push(findFloatToLeftOfOperator(i, expression));
					System.out.println("Dividing ON LEFT: "+values.peek());

					//Do the operation and put it on the stack
					values.push(values.pop()/values.pop());//Left value/Right Value
					low=findLowIndex(i, expression);
					hi=findHighIndex(i, expression);

					if(hi!=expression.length()-1)
					{
						if(!operators.isEmpty())
							expression=expression.substring(0,low)+operators.pop()+values.peek()+expression.substring(hi, expression.length());
						else
							expression=expression.substring(0,low)+values.peek()+expression.substring(hi, expression.length());

					}
					else
					{
						if(!operators.isEmpty())
							expression=expression.substring(0,low)+operators.pop()+values.peek();
						else
							expression=""+values.peek();


					}
					System.out.println("After Division symplifying: "+expression);

					i=0;
					break;

				}
				else if(expression.charAt(i)=='+')
				{
					operators.push('+');

					//System.out.println("+");

				}

				else if(i!=0&&expression.charAt(i)=='-'&&expression.charAt(i-1)>='0'&&expression.charAt(i-1)<='9')//Minus, but not negative
				{
					operators.push('-');

					System.out.println("Subtraction Added -");
				}
				else if(i!=0&&expression.charAt(i)=='-')
				{
					System.out.println("found -, No subtraction or Adding, value before - is: "+expression.charAt(i-1));
				}

			}
			/*expression="";
			while(!values.isEmpty()||!operators.isEmpty())
			{
				if(!values.isEmpty())
				expression+=""+values.pop();
				if(!operators.isEmpty())
				expression+=""+operators.pop(); 
			}*/
		}
		while(expression.contains("+")||expression.contains("-"))
		{
			//Check for only numbers left are negative numbers
			boolean isSubtractingOrAdding=true;
			String expressionCopy=expression;
			while(expressionCopy.contains("-"))
			{
				if(expressionCopy.indexOf('-')==0)
				{
					System.out.println("First Number is negative");
					expressionCopy=expressionCopy.substring(1);
					isSubtractingOrAdding=false;
					continue;
				}
				char c=expressionCopy.charAt(expressionCopy.indexOf('-')-1);
				System.out.println("Char before - is: "+c);
				if(c<'0'||c>'9')
				{
					System.out.println("Middle Number is Negative");
					isSubtractingOrAdding=false;
					break;
				}
				else
				{
					System.out.println("Subtracting in Middle");
					isSubtractingOrAdding=true;//Need to figure out what to do if you are subtracting, but you still have a negative number
					//Check case for (3-5)-2
					break;
				}
			}
			if(expression.contains("+"))
				isSubtractingOrAdding=true;
			if(isSubtractingOrAdding)//Only do the operation if you are subtracting or adding
			{
				low=0;hi=expression.length()-1;
				for(int i=0;i<expression.length();i++)
				{
					String strVal="";


					if(expression.charAt(i)=='+')
					{
						//Find the value to the right of the operator
						values.push(findFloatRightOfOperator(i, expression));

						System.out.println("ADDING TO RIGHT: "+values.peek());

						//Find value to the left of the operator		
						values.push(findFloatToLeftOfOperator(i, expression));
						System.out.println("ADDING TO LEFT: "+values.peek());


						//Do the operation and put it on the stack
						values.push(values.pop()+values.pop());//Left value*Right Value
						low=findLowIndex(i, expression);
						hi=findHighIndex(i, expression);
						if(hi!=expression.length()-1)
						{
							expression=expression.substring(0,low)+values.peek()+expression.substring(hi, expression.length());
						}
						else
						{
							expression=""+values.peek();
						}
						i=0;
						System.out.println("Simplified addition: "+expression);

					}

					else if(expression.charAt(i)=='-'&&i!=0)
					{
						//Find the value to the right of the operator
						values.push(findFloatRightOfOperator(i, expression));

						System.out.println("SUBTRACTING TO RIGHT: "+values.peek());

						//Find value to the left of the operator		
						values.push(findFloatToLeftOfOperator(i, expression));
						System.out.println("SUBTRACTING TO LEFT: "+values.peek());


						//Do the operation and put it on the stack
						values.push(values.pop()-values.pop());//Left value*Right Value
						low=findLowIndex(i, expression);
						hi=findHighIndex(i, expression);
						if(hi!=expression.length()-1)
						{
							expression=expression.substring(0,low)+values.peek()+expression.substring(hi, expression.length());
						}
						else
						{
							expression=""+values.peek();
						}
						i=0;
						System.out.println("Simplified subtraction: "+expression);
					}
				}
			}
			else
				break;

		}


		float value=0;
		try
		{
			value=Float.parseFloat(expression);
		}
		catch(Exception e)
		{

			System.out.println("ERROR: Could not parse: "+expression);
		}
		return value;


	}

	private String replaceVariablesWithScalar(String expression)
	{
		//HiVarIndex is index after variable
				int loVarIndex=-1, varIndex=0;
				String strVal="";
				boolean lastToken=false;
				while(varIndex<expression.length()&&(!scalars.isEmpty()||!arrays.isEmpty()))
				{
					
					//Find Variable
					while(varIndex<expression.length()&&((expression.charAt(varIndex)>='A'&&expression.charAt(varIndex)<='Z')||(expression.charAt(varIndex)>='a'&&expression.charAt(varIndex)<='z')))
					{
						if(loVarIndex==-1)
							loVarIndex=varIndex;
						strVal+=expression.charAt(varIndex);
						System.out.println("Variable is: "+strVal);
						if(varIndex+1<expression.length())
						{
							varIndex++;
						}
						else{
							lastToken=true;
							break;
						}
					}

					//Make hi=varIndex if lo is not -1
					if(loVarIndex!=-1)
					{
						//Replace the variable with its value
					//	hiVarIndex=varIndex;
						
						//Var is array
						if(expression.charAt(varIndex)=='[')
						{
							int firstBracket=varIndex;
							while(expression.charAt(varIndex)!=']')
							{
								varIndex++;
							}
							int secondBracket=varIndex;

							int arrayValueIndex=(int)evaluateRecursively(expression.substring(firstBracket+1,secondBracket));
							//Recursively evaluate
							System.out.println("ADDING VALUE: "+arrays.get(arrays.indexOf(new ArraySymbol(strVal))).values[arrayValueIndex] );
						//	if(!lastToken)
							expression=expression.substring(0, loVarIndex)+arrays.get(arrays.indexOf(new ArraySymbol(strVal))).values[arrayValueIndex]+expression.substring(varIndex+1);
							/*else
								expression=expression.substring(0, loVarIndex)+arrays.get(arrays.indexOf(new ArraySymbol(strVal))).values[arrayValueIndex];
*/
							if(!expression.contains(strVal))
							{
								System.out.println("Removing "+arrays.get(arrays.indexOf(new ArraySymbol(strVal))));
								scalars.remove(arrays.get(arrays.indexOf(new ArraySymbol(strVal))));
							}
						}
						else 
						{
							System.out.println("ADDING VALUE: "+scalars.get(scalars.indexOf(new ScalarSymbol(strVal))).value );
							if(!lastToken)
							expression=expression.substring(0, loVarIndex)+scalars.get(scalars.indexOf(new ScalarSymbol(strVal))).value+expression.substring(varIndex);
							else
								expression=expression.substring(0, loVarIndex)+scalars.get(scalars.indexOf(new ScalarSymbol(strVal))).value;

							if(!expression.contains(strVal))
							{
								System.out.println("Removing "+scalars.get(scalars.indexOf(new ScalarSymbol(strVal))));
								scalars.remove(scalars.get(scalars.indexOf(new ScalarSymbol(strVal))));
							}
						}
						varIndex=-1;
						loVarIndex=-1;
						strVal="";
						lastToken=false;
						
					}
								
					varIndex++;
				}
				return expression;
	}
	
	private float findFloatRightOfOperator(int i, String expression)
	{
		//Find the value to the right of the operator
		String strVal="";
		int newIndex=i+1;
		int highestIndex;
		//If right number is a negative number
		if(expression.charAt(newIndex)=='-')
		{
			newIndex++;
			strVal+="-";
			highestIndex=findHighIndex(i+1, expression);
		}
		else//Not negative
		{
			highestIndex=findHighIndex(i, expression);
		}
		//	while(expression.charAt(newIndex)>='0'&&expression.charAt(newIndex)<='9')
		System.out.println("Right Value: After "+expression.charAt(newIndex-1)+" but Before: "+expression.charAt(highestIndex));
		while(newIndex<highestIndex)
		{
			if((expression.charAt(newIndex)>='0'&&expression.charAt(newIndex)<='9')||expression.charAt(newIndex)=='.')
				strVal+=""+expression.charAt(newIndex);


			if(newIndex<expression.length())//might be newIndex+1 if you get an error
				newIndex++;
			else
				break;


		}
		if(highestIndex==expression.length()-1||strVal.equals(""))
			strVal+=expression.charAt(expression.length()-1);

		System.out.println("Right Value: "+strVal);
		return (Float.parseFloat(strVal));//Return value to te right




	}
	private Float findFloatToLeftOfOperator(int i, String expression)
	{
		//Find value to the left
		String strVal="";
		int newIndex=i-1;
		boolean negativeNumber=false;
		//while(expression.charAt(newIndex)>='0'&&expression.charAt(newIndex)<='9')
		int lowIndex=findLowIndex(i,expression);

		//Check if number is negative, then include that number in the operation
		if(lowIndex==0&&expression.charAt(lowIndex)=='-' )
		{
			System.out.println("Found negative number at start");
			negativeNumber=true;
		}
		else if(newIndex!=0&&expression.charAt(newIndex)=='-'&&expression.charAt(newIndex-1)<'0'&&expression.charAt(newIndex-1)>'9'&&expression.charAt(newIndex-1)!='.')
		{

			System.out.println("Found negative number");
			negativeNumber=true;
		}


		System.out.println("Left Value: Before "+expression.charAt(newIndex)+" but After: "+expression.charAt(lowIndex));

		while(newIndex>=lowIndex)//
		{
			if((expression.charAt(newIndex)>='0'&&expression.charAt(newIndex)<='9')||expression.charAt(newIndex)=='.')
				strVal=expression.charAt(newIndex)+strVal;

			if(newIndex-1>=0)
				newIndex--;
			else
				break;


		}
		if(negativeNumber)
			strVal="-"+strVal;
		System.out.println("Left Value: "+strVal);

		return (Float.parseFloat(strVal));//return value to the left
	}

	private int findLowIndex(int i, String expression)
	{

		int	newIndex=i-1;

		while(expression.charAt(newIndex)>='0'&&expression.charAt(newIndex)<='9'||expression.charAt(newIndex)=='.')
		{
			if(newIndex-1>=0)// need >=
				newIndex--;
			else  
				break;
		}

		return newIndex;

	}
	private int findHighIndex(int i, String expression)
	{
		int newIndex=i;
		//if(i+1<expression.length())

		newIndex=i+1;
		if(expression.charAt(newIndex)=='-')
		{
			newIndex++;
		}
		while(expression.charAt(newIndex)>='0'&&expression.charAt(newIndex)<='9'||expression.charAt(newIndex)=='.')
		{

			if(newIndex+1<expression.length())
				newIndex++;
			else
				break;
		}
		return newIndex;
	}
	/**
	 * Utility method, prints the symbols in the scalars list
	 */
	public void printScalars() {
		for (ScalarSymbol ss: scalars) {
			System.out.println(ss);
		}
	}

	/**
	 * Utility method, prints the symbols in the arrays list
	 */
	public void printArrays() {
		for (ArraySymbol as: arrays) {
			System.out.println(as);
		}
	}

}
