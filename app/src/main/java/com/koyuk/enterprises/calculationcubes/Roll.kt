package com.koyuk.enterprises.calculationcubes

import android.support.v7.app.AppCompatActivity
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import kotlinx.android.synthetic.main.roll.*
import android.media.SoundPool
import android.media.AudioAttributes
import android.os.*
import java.lang.Double.NaN
import java.lang.Double.POSITIVE_INFINITY
import kotlin.math.pow
import kotlin.math.roundToInt
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import kotlinx.android.synthetic.main.settings_view.view.*


/**
 * Dice code adapted from https://tekeye.uk/android/examples/android-diceImages-code
 *
 **/
class Roll : AppCompatActivity() {
    // TODO: make this a setting
    var numOfDice = 3
    var targetMax = 144
    var showPlusMinus = false
    var cutoff = 10000

    lateinit var die: MutableList<Die>
    lateinit var equations: ArrayList<Equation>
    lateinit var answers : ArrayList<Answer>
    lateinit var infinites : ArrayList<Answer>
    lateinit var nans : ArrayList<Answer>
    lateinit var settings: View
    lateinit var upDownButton: MenuItem

    var operations : ArrayList<String> = arrayListOf("+", "-", "*", "/", "^", "~")

    var rnd = Random()
    var diceSound: SoundPool? = null
    var soundId: Int = 0
    var handler: Handler? = null
    var timer = Timer()
    var rolling = false

    var target = 0
    var baseline = 0

    var isUp = true

    lateinit var emptyDieArray : MutableList<Die>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.roll)
        setSupportActionBar(findViewById(R.id.toolbar))
        //getSupportActionBar()!!.setDisplayShowTitleEnabled(false);

        //Our function to initialise sound playing
        rollButton!!.setOnClickListener(HandleClick())
        //link handler to callback
        handler = Handler(callback)
        answers = arrayListOf()
        die = mutableListOf()

        setDice()

        checkShowAnswers.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                scrollListView.visibility = View.VISIBLE
                if(answers.count() > 0) {
                    differenceText.visibility = View.VISIBLE
                }
            }
            else{
                scrollListView.visibility = View.INVISIBLE
                differenceText.visibility = View.INVISIBLE
            }
        })
        settings = findViewById(R.id.calcSettingsView)
        settings.bringToFront()

        var spinner = findViewById<Spinner>(R.id.numDiceSpinner)
        val spinnerdapter = ArrayAdapter.createFromResource(this, R.array.numDiceArray, R.layout.spinner_text)
        spinnerdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = spinnerdapter

        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                parent.getItemAtPosition(position)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        spinner.setSelection(spinnerdapter.getPosition( numOfDice.toString()))
        spinner.invalidate()
        spinner.bringToFront()

        var submitButton = findViewById<Button>(R.id.submitSettingsButton)
        submitButton.setOnClickListener {
            setSettings()
        }
        var cancelButton = findViewById<Button>(R.id.settingsCancelButton)
        cancelButton.setOnClickListener {
            slideUp()
        }

    }// Menu icons are inflated just as they were with actionbar

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        upDownButton = menu.findItem(R.id.settings_menu)
        return true
    }

    fun setDice(){
        emptyDieArray = mutableListOf()
        for (i in 0 until numOfDice) {
            emptyDieArray.add(Die(0))
        }

        val recyclerView = findViewById<RecyclerView>(R.id.diceView)
        val adapter = DiceRecyclerViewAdapter(emptyDieArray)

        recyclerView.adapter = adapter
        var gLayout : GridLayoutManager
        if(numOfDice % 3 == 0) {
            gLayout = GridLayoutManager(this, 3)
        }
        else if(numOfDice % 2 == 0) {
            gLayout = GridLayoutManager(this, 2)
        }
        else{
            gLayout = GridLayoutManager(this, 6)
            gLayout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    when (position % 5) {
                        0 -> return 2
                        1 -> return 2
                        2 -> return 2
                        3 -> return 3
                        4 -> return 3
                        else -> return -1
                    }
                }
            }
        }
        recyclerView.layoutManager = gLayout
    }

    fun setSettings(){
        var spinner = findViewById<Spinner>(R.id.numDiceSpinner)
        numOfDice = spinner.selectedItemPosition + 2
        answers = arrayListOf()
        setAnswersView()
        targetText.text = ""
        val maxTarget =  findViewById<EditText>(R.id.setMaxTarget).text.toString()
        try{
            targetMax = Integer.parseInt(maxTarget)
        }
        catch(e: Exception){
            setMaxTargetErrorVisible(true)
            return
        }
        setDice()
        slideUp()
    }
    fun setMaxTargetErrorVisible(visible: Boolean){

        val error =  findViewById<TextView>(R.id.setMaxTargetError)
        if(visible){
            error.visibility = View.VISIBLE
        }
        else{
            error.visibility = View.INVISIBLE
        }
    }

    public fun slideUp() {
        settings.animate()
                .translationY(0f)
                .setDuration(500)
                .start()
        upDownButton.setIcon(R.drawable.baseline_keyboard_arrow_down_black_18dp)
        isUp = !isUp

        var inout = AnimationUtils.makeInAnimation(this, false);
        rollButton.startAnimation(inout);
        rollButton.visibility = View.VISIBLE;
    }

    // slide the view from its current position to below itself
    public fun slideDown(){
        showCurrentSettings()
        settings.animate()
                .translationY(900f)
                .setDuration(500)
                .start()
        upDownButton.setIcon(R.drawable.baseline_keyboard_arrow_up_black_18dp)
        isUp = !isUp

        var out = AnimationUtils.makeOutAnimation(this, true);
        rollButton.startAnimation(out);
        rollButton.setVisibility(View.INVISIBLE);
    }

    public fun onSlideSettingsView(button: MenuItem) {
        if (isUp) {
            slideDown()
        } else {
            slideUp()
        }
    }
    fun showCurrentSettings(){
        var max = findViewById<EditText>(R.id.setMaxTarget)
        max.setText(targetMax.toString())
        setMaxTargetErrorVisible(false)
    }

    //User pressed diceImages, lets start
    private inner class HandleClick : View.OnClickListener {
        override fun onClick(arg0: View) {
            if (!rolling) {
                rolling = true
                //diceImages.forEach {
                //    it!!.setImageResource(R.drawable.dice3droll)
                //}
                //Start rolling sound
 //               diceSound!!.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
                //Pause to allow image to update
                timer.schedule(Roll(), 400)
            }
        }
    }

    //New code to initialise sound playback
    fun initSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Use the newer SoundPool.Builder
            //Set the audio attributes, SONIFICATION is for interaction events
            //uses builder pattern
            val aa = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

            //default max streams is 1
            //also uses builder pattern
            diceSound = SoundPool.Builder().setAudioAttributes(aa).build()

        } else {
            //Running on device earlier than Lollipop
            //Use the older SoundPool constructor
            diceSound = PreLollipopSoundPool.NewSoundPool()
        }
        //Load the diceImages sound
        soundId = diceSound!!.load(this, R.raw.shake_dice, 1)
    }

    //When pause completed message sent to callback
    internal inner class Roll : TimerTask() {
        override fun run() {
            handler!!.sendEmptyMessage(0)
        }
    }

    //Receives message from timer to start diceImages roll
    var callback: Handler.Callback = object : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            baseline = 0
            equations = ArrayList(10)
            answers = ArrayList(10)
            nans = ArrayList(10)
            infinites = ArrayList(10)
            answerListView.adapter = null
            die.clear()
            val recyclerView = findViewById<RecyclerView>(R.id.diceView)
            val adapter = DiceRecyclerViewAdapter(emptyDieArray)
            recyclerView.adapter = adapter
            //Get roll result
            for (i in 0 until numOfDice) {
                var oneDie = rnd.nextInt(6) + 1
                baseline += oneDie
                die.add(Die(oneDie))
                equations.add(SimpleNumber(oneDie))
            }
            target = rnd.nextInt(targetMax) + 1
            targetText!!.text = (target).toString()
            // setAnswers(one, two, three, target)
            rolling = false  //user can press again
            // TODO: in separate thread ("Only the original thread that created a view hierarchy can touch its views.")
            //thread { findAnswers() }
            findAnswers()
            if(checkShowAnswers.isChecked) {
                differenceText.visibility = View.VISIBLE
            }
            val adapter2 = DiceRecyclerViewAdapter(die)
            val callback = ItemMoveCallback(adapter2)
            if(numOfDice > 3){
                callback.setMoreThanOneRow()
            }
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(recyclerView)
            recyclerView.adapter = adapter2
            return true
        }
    }

    private fun findAnswers(){
        var results = computeThree(equations)
        for (i in 0 until results.size) {
            addToList(results[i])
        }
        // sort and show results
        setAnswersView()

        if(!checkShowAnswers.isChecked){
            scrollListView.visibility = View.INVISIBLE
            differenceText.visibility = View.INVISIBLE
        }
        // TODO: Not showing nans, infinities
    }
    private fun setAnswersView(){
        // sort and show results
        var display = answers.sortedWith(compareBy { it.absoluteDiff }).toMutableList()

        val recyclerView = findViewById(R.id.answerListView) as RecyclerView
        val adapter = TwoColumnListAdapter(display)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    private fun sortNumber(a:Equation, b:Equation): Int
    {
        var comp = Math.abs(target-a.solution) - Math.abs(target-b.solution);
        if(comp==0.0) return 0;
        if(comp>0) return -1;
        return 1;
    }

    private fun computeThree(dice: ArrayList<Equation>) : List<Equation>{
        var results = ArrayList<Equation>(20)
        for (i in 0 until dice.size) {
            var equationStart = equations[i];
            var rest = arrayListOf<Equation>()
            for (j in 0 until dice.size) {
                if (i != j) {
                    rest.add(equations[j]);
                }
            }
            var restResults = computeTwo(rest)
            for (i in 0 until restResults.size) {
                results.add(Add(equationStart, restResults[i]))
                results.add(Subtract(equationStart, restResults[i]))
                results.add(Subtract(restResults[i], equationStart))
                results.add(Multiply(equationStart, restResults[i]))
                results.add(Divide(equationStart, restResults[i]))
                results.add(Divide(restResults[i], equationStart))
                results.add(PowerOf(equationStart, restResults[i]))
                results.add(PowerOf(restResults[i], equationStart))
                results.add(RootOf(equationStart, restResults[i]))
                results.add(RootOf(restResults[i], equationStart))
            }
        }
        return results.filter{it.solution <= cutoff}.distinctBy { it.equation }
    }
    private fun computeTwo(dice: ArrayList<Equation>) : List<Equation>{
        var die0 = dice[0]
        var die1 = dice[1]
        var results = ArrayList<Equation>(10)

        results.add(Add(die0, die1))
        results.add(Subtract(die0, die1))
        results.add(Multiply(die0, die1))
        results.add(Divide(die0, die1))
        results.add(PowerOf(die0, die1))
        results.add(RootOf(die0, die1))

        if(die0.equation != die1.equation) {
            results.add(Subtract(die1, die0))
            results.add(Divide(die1, die0))
            results.add(PowerOf(die1, die0))
            results.add(RootOf(die1, die0))
        }
        return results.filter{it.solution <= cutoff}.distinctBy { it.equation }
    }

    private fun addToList(equation: Equation) {

//	if(Math.abs(currentEquation.solution - goal) <= Math.abs(baseline - goal) )
//	{
        // this is lame, have to find a way to prevent duplicates to begin with
        var answer = Answer(equation, target)
        if (answer.solution == POSITIVE_INFINITY || answer.solution == Double.NEGATIVE_INFINITY ) {
            infinites.add(answer)
        } else if (answer.solution == NaN) {
            nans.add(answer)
        } else {
            answers.add(answer)
        }
//	}
    }




    //Clean up
    override fun onPause() {
        super.onPause()
//        diceSound!!.pause(soundId)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    abstract class Equation {
        var operator : String = ""
        var equation : String = ""
        var solution : Double = 0.0
        var isAllPlus : Boolean = false
        var isAllTimes : Boolean = false

        constructor(a: Int) {
            solution = a.toDouble()
            equation = a.toString()
            isAllPlus = true
            isAllTimes = true
        }
        constructor(a: Equation, b: Equation, c: String){
            this.operator = c
            computeSolution(a, b)
            computeText(a, b)
            this.isAllPlus = this.isAllPlus && a.isAllPlus && b.isAllPlus
            this.isAllTimes = this.isAllTimes && a.isAllTimes && b.isAllTimes
        }

        protected open fun computeSolution(a: Equation, b: Equation){}

        protected open fun computeText(a: Equation, b: Equation) {
            if(a is SimpleNumber){
                if(b is SimpleNumber){
                    equation = a.equation + this.operator + b.equation
                }
                else{
                    equation = a.equation + this.operator + "(" + b.equation + ")"
                }
            }
            else{
                if(b is SimpleNumber){
                    equation = "(" + a.equation + ")" + this.operator + b.equation
                }
                else{
                    equation = "(" + a.equation + ")" + this.operator + "(" + b.equation + ")"
                }
            }
        }
    }

    class SimpleNumber : Equation {
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation, c: String) : super(a, b, c)
    }

    class Add : Equation {
        init {
            this.isAllPlus = true
        }
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation) : super(a, b, "+")
        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution + b.solution
        }
    }

    class Subtract : Equation {
        init {
        }
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation) : super(a, b, "-")
        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution - b.solution
        }
    }

    class Multiply : Equation {
        init {
            this.isAllTimes = true
        }
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation) : super(a, b, "*")
        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution * b.solution
        }
    }

    class Divide : Equation {
        init {
        }
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation) : super(a, b, "/")
        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution / b.solution
        }
    }

    class PowerOf : Equation {
        init {
        }
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation) : super(a, b, "^")
        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution.pow(b.solution)
        }

        override fun computeText(a: Equation, b: Equation) {
            equation = if(a is SimpleNumber){
                if(b is SimpleNumber){
                    a.equation + "<sup>" + b.equation + "</sup>"
                } else{
                    a.equation + "<sup>" + "(" + b.equation + ")" + "</sup>"
                }
            } else{
                if(b is SimpleNumber){
                    "(" + a.equation + ")" + "<sup>" + b.equation + "</sup>"
                } else{
                    "(" + a.equation + ")" + "<sup>" + "(" + b.equation + ")" + "</sup>"
                }
            }
        }
    }

    class RootOf : Equation {
        init {
        }
        constructor(a: Int) : super(a)
        constructor(a: Equation, b: Equation) : super(a, b, "~")
        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution.pow(1/b.solution)
        }

        override fun computeText(a: Equation, b: Equation) {
            equation = if(a is SimpleNumber){
                if(b is SimpleNumber){
                    a.equation + "<sup>1/" + b.equation + "</sup>"
                } else{
                    a.equation + "<sup>1/" + "(" + b.equation + ")" + "</sup>"
                }
            } else{
                if(b is SimpleNumber){
                    "(" + a.equation + ")" + "<sup>1/" + b.equation + "</sup>"
                } else{
                    "(" + a.equation + ")" + "<sup>1/" + "(" + b.equation + ")" + "</sup>"
                }
            }
        }
    }

    inner class Answer{
        var display : String = ""
        var solution : Double = 0.0
        var absoluteDiff: Double = 0.0
        var absoluteDiffDisplay: String = ""
        constructor(eq: Equation, target: Int){
            solution = eq.solution
            display = display(eq)
            var diff = target - solution
            if(showPlusMinus) {
                absoluteDiff = Math.abs(diff)
                absoluteDiffDisplay = tryRoundSolution(diff)
                if (diff > 0) {
                    absoluteDiffDisplay = "+" + absoluteDiffDisplay
                }
            }
            else{
                absoluteDiff = Math.abs(diff)
                absoluteDiffDisplay = tryRoundSolution(absoluteDiff)
            }
        }

        fun display(eq : Equation) = eq.equation + " = " + tryRoundSolution(eq.solution)

        private fun tryRoundSolution(sol : Double) : String {
            if(sol.isNaN() || sol.isInfinite()){
                return sol.toString()
            }
            var rounded = sol.roundToInt()
            if(sol - rounded != 0.0) {
                return "%.2f".format(sol)
            }
            return rounded.toString()
        }
    }

    fun <Int> permute(list:List <Int>):List<List<Int>>{
        if(list.size==1) return listOf(list)
        val perms=mutableListOf<List <Int>>()
        val sub=list[0]
        for(perm in permute(list.drop(1)))
            for (i in 0..perm.size){
                val newPerm=perm.toMutableList()
                newPerm.add(i,sub)
                perms.add(newPerm)
            }
        return perms.distinct()
    }
}
