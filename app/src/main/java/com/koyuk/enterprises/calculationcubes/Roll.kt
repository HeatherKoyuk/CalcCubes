package com.koyuk.enterprises.calculationcubes

import android.support.v7.app.AppCompatActivity
import android.view.View
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import kotlinx.android.synthetic.main.roll.*
import android.media.SoundPool
import android.media.AudioAttributes
import android.os.*
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.widget.ImageView
import android.widget.TextView
import android.widget.ArrayAdapter
import java.lang.Double.NaN
import java.lang.Double.POSITIVE_INFINITY
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.roundToInt
import android.widget.Toast
import android.widget.CompoundButton
import android.view.ViewGroup
import android.view.View.OnLayoutChangeListener
import android.support.v7.widget.LinearLayoutManager
import android.R.attr.data
import android.support.v7.widget.RecyclerView








/**
 * Dice code adapted from https://tekeye.uk/android/examples/android-diceImages-code
 *
 **/
class Roll : AppCompatActivity() {
    // TODO: make this a setting
    var numOfDice = 3
    var targetMax = 144

    lateinit var diceImages: List<ImageView>
    lateinit var diceText: List<TextView>

    lateinit var equations: ArrayList<Equation>
    lateinit var answers : ArrayList<Answer>
    lateinit var infinites : ArrayList<Answer>
    lateinit var nans : ArrayList<Answer>

    var operations : ArrayList<String> = arrayListOf("+", "-", "*", "/", "^", "~")

    var showPlusMinus = false

    var rnd = Random()
    var diceSound: SoundPool? = null
    var soundId: Int = 0
    var handler: Handler? = null
    var timer = Timer()
    var rolling = false

    var target = 0
    var baseline = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.roll)
        //Our function to initialise sound playing
        rollButton!!.setOnClickListener(HandleClick())
        //link handler to callback
        handler = Handler(callback)

        diceImages = listOf(die1, die2, die3)
        diceText = listOf(die1Text, die2Text, die3Text)
        checkShowAnswers.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                scrollListView.visibility = View.VISIBLE
                differenceText.visibility = View.VISIBLE
            }
            else{
                scrollListView.visibility = View.INVISIBLE
                differenceText.visibility = View.INVISIBLE
            }
        })
    }

    //User pressed diceImages, lets start
    private inner class HandleClick : View.OnClickListener {
        override fun onClick(arg0: View) {
            if (!rolling) {
                rolling = true
                diceImages.forEach {
                    it!!.setImageResource(R.drawable.dice3droll)
                }
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
            //Get roll result
            for (i in 0 until diceImages.size) {
                var die = rnd.nextInt(6) + 1
                baseline += die
                setDieImage(diceImages[i]!!, diceText[i]!!, die)
                equations.add(SimpleNumber(die))
            }
            target = rnd.nextInt(targetMax) + 1
            targetText!!.text = (target).toString()
            // setAnswers(one, two, three, target)
            rolling = false  //user can press again
            // TODO: in separate thread ("Only the original thread that created a view hierarchy can touch its views.")
            //thread { findAnswers() }
            findAnswers()
            return true
        }
    }

    private fun findAnswers(){
        var results = computeThree(equations)
        for (i in 0 until results.size) {
            addToList(results[i])
        }
        // sort and show results
        var display = answers.sortedWith(compareBy { it.absoluteDiff }).toMutableList()

        val recyclerView = findViewById(R.id.answerListView) as RecyclerView
        val adapter = TwoColumnListAdapter(display)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        if(!checkShowAnswers.isChecked){
            scrollListView.visibility = View.INVISIBLE
            differenceText.visibility = View.INVISIBLE
        }
        // TODO: Not showing nans, infinities
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
        return results.distinctBy { it.solution }
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
        return results.distinctBy { it.solution }
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



    private fun setDieImage(img: ImageView, txt: TextView, random: Int){
        when (random) {
            1 -> img.setImageResource(R.drawable.one)
            2 -> img.setImageResource(R.drawable.two)
            3 -> img.setImageResource(R.drawable.three)
            4 -> img.setImageResource(R.drawable.four)
            5 -> img.setImageResource(R.drawable.five)
            6 -> img.setImageResource(R.drawable.six)
        }
        txt.text = (random).toString()
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
