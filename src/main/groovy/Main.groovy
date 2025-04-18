
static void main(String[] args) {
    def choicePrompt = "Choose mode:\n1. Symbolic Integration using U-Substitution\n2. Numerical Integration using Simpson's Rule\nEnter choice 1 or 2: "
    int choice = readIntInput(choicePrompt)
    switch (choice) {
        case 1:
            handleSymbolicIntegration()
            break
        case 2:
            handleNumericalIntegration()
            break
        default:
            println("We only expected 1 or 2.")
    }
}

static void handleSymbolicIntegration() {
    String function = readStringInput("Enter function ('x^2', 'e^3x', 'sin(x)'): ")
    println "∫ $function dx = ${integrate(function)} + C"
}

static void handleNumericalIntegration() {
    def (function, a, b, n) = getIntegrationInput()
    Closure<Double> f = createFunctionClosure(function)
    double result = simpsonsRule(f, a, b, n)
    println "∫[$a, $b] $function dx ≈ $result"
}

static Closure<Double> createFunctionClosure(String function) {
    return { x ->
        Binding binding = new Binding()
        binding.setVariable("x", x)
        GroovyShell shell = new GroovyShell(binding)
        try {
            return shell.evaluate(function) as Double
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to evaluate function '$function' at x=$x")
        }
    }
}

static double simpsonsRule(Closure<Double> f, double a, double b, int n) {
    if (n % 2 != 0) n++
    double h = (b - a) / n
    double sum = f(a) + f(b)
    (1..<n).each { i ->
        double x = a + i * h
        sum += (i % 2 == 0) ? 2 * f(x) : 4 * f(x)
    }
    return (h / 3) * sum
}

static def getIntegrationInput() {
    String function = readStringInput("Enter function (Math.sin(x), Math.cos(x), Math.exp(x), x*x): ")
    double a = readDoubleInput("Enter lower bound (a): ")
    double b = readDoubleInput("Enter upper bound (b): ")
    int n = (int) readDoubleInput("Enter number of intervals (n): ")
    return [function, a, b, n]
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
            'constant'   : { String expr ->
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

    for (rule in integrationRules.values()) {
        def result = rule(integrand)
        if (result != null) return result
    }

    def uSubResult = uSubstitution(integrand)
    if (uSubResult != null) return uSubResult

    return "Could not find integration method for: $integrand"
}

static String uSubstitution(String integrand) {
    def patterns = [
            /(\d+)x\^(\d+)/      : { m ->
                def coeff = m[0][1] ? m[0][1] as Integer : 1
                def power = m[0][2] as Integer
                return "(${coeff}/${power + 1})x^${power + 1}"
            },
            /e\^(\d+)x/          : { m ->
                def coeff = m[0][1] ? m[0][1] as Integer : 1
                return "(1/${coeff})e^${coeff}x"
            },
            /(sin|cos)\((\d+)x\)/: { m ->
                def func = m[0][1]
                def coeff = m[0][2] as Integer
                return func == "sin" ? "(-1/${coeff})cos(${coeff}x)" : "(1/${coeff})sin(${coeff}x)"
            }
    ]

    for (pattern in patterns) {
        def matcher = (integrand =~ pattern.key)
        if (matcher.matches()) {
            return pattern.value(matcher)
        }
    }
    return null
}

static String readStringInput(String prompt) {
    print(prompt)
    return new Scanner(System.in).nextLine().trim()
}

static double readDoubleInput(String prompt) {
    Scanner scanner = new Scanner(System.in)
    while (true) {
        try {
            print(prompt)
            return scanner.nextDouble()
        } catch (Exception e) {
            println("Invalid input! Please enter a valid value.")
            scanner.next() // Clear invalid input
        }
    }
}

static int readIntInput(String prompt) {
    Scanner scanner = new Scanner(System.in)
    while (true) {
        try {
            print(prompt)
            return scanner.nextInt()
        } catch (Exception e) {
            println("Invalid input! Please enter a valid value.")
            scanner.next() 
        }
    }
}

