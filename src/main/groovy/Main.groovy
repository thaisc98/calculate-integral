static double simpsonsRule(Closure<Double> f, double a, double b, int n) {
    if (n % 2 != 0) {
        n++
    }

    double h = (b - a) / n
    double sum = f(a) + f(b)

    (1..<n).each { i ->
        double x = a + i * h
        sum += (i % 2 == 0) ? 2 * f(x) : 4 * f(x)
    }

    return (h / 3) * sum
}

def static readFunctionInput() {
    print("Enter function (Math.sin(x), Math.cos(x), Math.exp(x), x*x): ")
    return new Scanner(System.in).nextLine().trim()
}

def static readDoubleInput(String prompt) {
    Scanner scanner = new Scanner(System.in)
    while (true) {
        try {
            print(prompt)
            return scanner.nextDouble()
        } catch (Exception e) {
            println("Invalid input! Please enter a number.")
            scanner.next() // Clear invalid input
        }
    }
}

def static getIntegrationInput() {
    def function = readFunctionInput()
    // Read bounds (for definite integral)
    double a = readDoubleInput("Enter lower bound (a): ")
    double b = readDoubleInput("Enter upper bound (b): ")

    // Read number of intervals (for numerical methods)
    int n = (int) readDoubleInput("Enter number of intervals (n): ")

    return [function, a, b, n]
}

// Attempt u-substitution
static String uSubstitution(String integrand) {
    // common u-sub cases
    def patterns = [
            // Polynomial: kx^n
            /(\d+)x\^(\d+)/      : { m ->
                def coeff = m[0][1] ? m[0][1] as Integer : 1
                def power = m[0][2] as Integer
                return [
                        u     : "x^${power + 1}",
                        du    : "${(power + 1) * coeff}x^${power}",
                        result: "(${coeff}/${power + 1})x^${power + 1}"
                ]
            },
            // Exponential: e^kx
            /e\^(\d+)x/          : { m ->
                def coeff = m[0][1] ? m[0][1] as Integer : 1
                return [
                        u     : "e^${coeff}x",
                        du    : "${coeff}e^${coeff}x",
                        result: "(1/${coeff})e^${coeff}x"
                ]
            },
            // Trig: sin(kx) or cos(kx)
            /(sin|cos)\((\d+)x\)/: { m ->
                def func = m[0][1]
                def coeff = m[0][2] as Integer
                return [
                        u: "${func}(${coeff}x)",
                        du: "${coeff}${func == 'sin' ? 'cos' : '-sin'}(${coeff}x)",
                        result: func == "sin" ? "(-1/${coeff})cos(${coeff}x)" : "(1/${coeff})sin(${coeff}x)"
                ]
            }
    ]

    for (pattern in patterns) {
        def matcher = (integrand =~ pattern.key)
        if (matcher.matches()) {
            def substitution = pattern.value(matcher)
            return substitution.result
        }
    }
    return null
}

static String integrate(String integrand) {
    Map<String, Closure> integrationRules = [
            'trig'       : { String expr ->
                def trigFuncs = [
                        'sin(x)': '-cos(x)',
                        'cos(x)': 'sin(x)',
                        'tan(x)': '-ln|cos(x)|'
                ]
                return trigFuncs[expr]
            },
            'constant'    : { String expr ->
                if (expr == '1') return 'x'
                if (expr.matches(/^(\d+|\d+\.\d+)$/)) {
                    def coeff = expr as double
                    return "(${coeff}x)"
                }
                return null
            },
            'power'      : { String expr ->
                if (expr.matches(/x\^\-?\d+/)) {
                    def power = expr.split('\\^')[1] as int
                    def newPower = power + 1
                    return "(1/${newPower})x^${newPower}"
                }
                return null
            },
            'exponential': { String expr ->
                if (expr == 'e^x') return 'e^x'
                if (expr.matches(/e\^\d+x/)) {
                    def coeff = expr.split('\\^')[1].replace('x', '') as int
                    return "(1/${coeff})${expr}"
                }
                return null
            }

    ]

    // basic integration rules
    for (rule in integrationRules.values()) {
        def result = rule(integrand)
        if (result != null) return result
    }

    //  if basic rules don't apply
    def uSubResult = uSubstitution(integrand)
    if (uSubResult != null) return uSubResult

    return "Could not find integration method for: $integrand"
}


static void main(String[] args) {
    println("Choose mode:")
    println("1. Symbolic Integration using U-Substitution")
    println("2. Numerical Integration using Simpsons Rule")
    print("Enter choice 1 or 2: ")
    def choice;
    try {
        choice = new Scanner(System.in).nextInt()
    } catch (InputMismatchException e1) {
        println("Invalid input! Please enter a number.")
        return
    }
    if (choice == 1) {
        print("Enter function ('x^2', 'e^3x', 'sin(x)'): ")
        String function = new Scanner(System.in).nextLine().trim()
        println "∫ $function dx = ${integrate(function)} + C"
    } else if (choice == 2) {
        def (function, a, b, n) = getIntegrationInput()
        Closure<Double> f = { x ->
            Binding binding = new Binding()
            binding.setVariable("x", x)
            GroovyShell shell = new GroovyShell(binding)
            try {
                return shell.evaluate(function) as Double
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to evaluate function '$function' at x=$x")
            }
        }
        double result = simpsonsRule(f, a, b, n)
        println "∫[$a, $b] $function dx ≈ $result"
    } else {
        println("We only expected 1 or 2.")
    }
}

