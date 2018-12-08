package com.koyuk.enterprises.calculationcubes

import android.content.Context
import android.support.v7.app.AppCompatActivity
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
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import kotlinx.android.synthetic.main.settings_view.*
import kotlinx.android.synthetic.main.settings_view.view.*
import java.util.*
import android.os.AsyncTask;
import android.util.Log
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.koyuk.enterprises.calculationcubes.Roll.GlobalVariable.lookup
import com.google.android.gms.ads.MobileAds
import com.koyuk.enterprises.calculationcubes.R.layout.roll

/**
 * Dice code adapted from https://tekeye.uk/android/examples/android-diceImages-code
 *
 **/
class Roll : AppCompatActivity(), OnStartDragListener, OnDiceListChangedListener {
    // TODO: make this a setting
    var numOfDice = 3
    var numOfSides = 6
    var targetMax = 144
    var answerListMaxSize = 500
    var allowPowers = true
    var hideSolutions = true

    lateinit var die: MutableList<Die>
    lateinit var equations: ArrayList<Equation>
    lateinit var answers: ArrayList<Answer>
    //lateinit var infinites: ArrayList<Answer>
    //lateinit var nans: ArrayList<Answer>
    lateinit var settings: View
    lateinit var upDownButton: MenuItem

    var operations: ArrayList<String> = arrayListOf("+", "-", "*", "/", "^", "~")

    var rnd = Random()
    var diceSound: SoundPool? = null
    var soundId: Int = 0
    var handler: Handler? = null
    var timer = Timer()
    var rolling = false

    var target = 0
    var baseline = Double.MAX_VALUE

    var isUp = true

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: DiceRecyclerViewAdapter? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mItemTouchHelper: ItemTouchHelper? = null
    private var mTask: LongOperation = LongOperation()

    lateinit var emptyDieArray: MutableList<Die>

    var rolls = 0
    var rollsBeforeAds = 8
    private lateinit var mInterstitialAd: InterstitialAd

    object GlobalVariable {
        var lookup: MutableMap<String, Equation> = mutableMapOf()
        var context: Context? = null
        var combinations: MutableMap<String, List<IntArray>> = mutableMapOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalVariable.context = this
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

        setupRecyclerView()

        hideSolutionsCB.isChecked = hideSolutions

        checkShowAnswers.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                scrollListView.visibility = View.VISIBLE
                if(mTask.getStatus() == AsyncTask.Status.RUNNING) {
                    // My AsyncTask is currently doing work in doInBackground()
                    indeterminateBar.visibility = View.VISIBLE
                    differenceText.visibility = View.INVISIBLE
                }
                else if (answers.count() > 0) {
                    differenceText.visibility = View.VISIBLE
                }
                else{
                    differenceText.visibility = View.INVISIBLE
                }
            } else {
                scrollListView.visibility = View.INVISIBLE
                differenceText.visibility = View.INVISIBLE
            }
        }
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
        spinner.setSelection(spinnerdapter.getPosition(numOfDice.toString()))


        var spinnerSides = findViewById<Spinner>(R.id.numDiceSidesSpinner)
        val spinneradapterSides = ArrayAdapter.createFromResource(this, R.array.numDiceSidesArray, R.layout.spinner_text)
        spinneradapterSides.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerSides.adapter = spinneradapterSides

        spinnerSides.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                parent.getItemAtPosition(position)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        spinnerSides.setSelection(spinneradapterSides.getPosition(numOfSides.toString()))

        var submitButton = findViewById<Button>(R.id.submitSettingsButton)
        submitButton.setOnClickListener {
            setSettings()
        }
        var cancelButton = findViewById<Button>(R.id.settingsCancelButton)
        cancelButton.setOnClickListener {
            slideUp()
        }

        MobileAds.initialize(this, "ca-app-pub-4140639688578770~6032762053")
        mInterstitialAd = InterstitialAd(this)
        //mInterstitialAd.adUnitId = "ca-app-pub-4140639688578770/2023851391"
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

    }// Menu icons are inflated just as they were with actionbar

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        upDownButton = menu.findItem(R.id.settings_menu)
        return true
    }

    private fun setupRecyclerView() {
        mRecyclerView = findViewById(R.id.diceView)
        mRecyclerView?.setHasFixedSize(true);
        var gLayout: GridLayoutManager
        if (numOfDice % 3 == 0) {
            gLayout = GridLayoutManager(this, 3)
        } else if (numOfDice % 2 == 0) {
            gLayout = GridLayoutManager(this, 2)
        } else {
            gLayout = GridLayoutManager(this, 6)
            gLayout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (position % 5) {
                        0 -> 2
                        1 -> 2
                        2 -> 2
                        3 -> 3
                        4 -> 3
                        else -> -1
                    }
                }
            }
        }
        mLayoutManager = gLayout
        mRecyclerView?.layoutManager = mLayoutManager

        //setup the adapter with empty list
        mAdapter = DiceRecyclerViewAdapter(getEmptyDie(), this, this, this)
        val callback = ItemMoveCallback(mAdapter!!)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(mRecyclerView)

        mRecyclerView?.adapter = mAdapter
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper?.startDrag(viewHolder)
    }

    fun getEmptyDie(): MutableList<Die> {
        differenceText.visibility = View.INVISIBLE

        emptyDieArray = mutableListOf()
        for (i in 0 until numOfDice) {
            emptyDieArray.add(Die(0))
        }
        maxTargetText.text = "/" + targetMax

        return emptyDieArray
    }

    private fun setSettings() {
        hideSolutions = hideSolutionsCB.isChecked
        if (hideSolutions) {
            checkShowAnswers.isChecked = false
        }
        val maxTarget = findViewById<EditText>(R.id.setMaxTarget).text.toString()
        try {
            val tempTargetMax = Integer.parseInt(maxTarget)
            if (tempTargetMax > 9999) {
                findViewById<TextView>(R.id.setMaxTargetError).text = "Max Target must be less than 9999"
                setMaxTargetErrorVisible(true)
                return
            }
            targetMax = tempTargetMax
            maxTargetText.text = "/" + targetMax
        } catch (e: Exception) {
            findViewById<TextView>(R.id.setMaxTargetError).text = "Enter a valid integer"
            setMaxTargetErrorVisible(true)
            return
        }
        val prevAllowPowers = allowPowers
        allowPowers = allowPowersCB.isChecked

        var spinner = findViewById<Spinner>(R.id.numDiceSpinner)
        val prevNumberDice = numOfDice
        numOfDice = spinner.selectedItemPosition + 2

        if (prevNumberDice != numOfDice) {
            if(mTask.getStatus() == AsyncTask.Status.RUNNING){
                indeterminateBar.visibility = View.INVISIBLE
                mTask.cancel(true)
            }
            answers = arrayListOf()
            setAnswersView()
            targetText.text = ""
            die.clear()
            die = getEmptyDie()
            setDice()
        } else {
            if (prevAllowPowers != allowPowers) {
                if(mTask.getStatus() == AsyncTask.Status.RUNNING){
                    var success = mTask.cancel(true)
                    //mTask.
                }

                setBlankAnswersView()
                mTask = LongOperation()
                mTask.execute()
            }
        }
        var spinnerSides = findViewById<Spinner>(R.id.numDiceSidesSpinner)
        numOfSides = spinnerSides.selectedItemPosition + 2

        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0)
        } catch (e: Exception) {

        }

        slideUp()
    }

    fun setMaxTargetErrorVisible(visible: Boolean) {

        val error = findViewById<TextView>(R.id.setMaxTargetError)
        if (visible) {
            error.visibility = View.VISIBLE
        } else {
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
    fun slideDown() {
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

    fun showCurrentSettings() {
        var max = findViewById<EditText>(R.id.setMaxTarget)
        max.setText(targetMax.toString())
        setMaxTargetErrorVisible(false)
        numDiceSpinner.setSelection(numOfDice - 2)
        numDiceSidesSpinner.setSelection(numOfSides - 2)
        allowPowersCB.isChecked = allowPowers
        hideSolutionsCB.isChecked = hideSolutions
    }

    //User pressed diceImages, lets start
    private inner class HandleClick : View.OnClickListener {
        override fun onClick(arg0: View) {
            if (rolls >= rollsBeforeAds ) {
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                }
                rolls = 0
            }
            else{
                rolls ++;
            }
            if(mTask.status == AsyncTask.Status.RUNNING){
                indeterminateBar.visibility = View.INVISIBLE
                mTask.cancel(true)
            }
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

    private inner class LongOperation : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String {
            try {
                lookup = mutableMapOf()
                answers = arrayListOf()
                compute(equations, true)
            } catch (e: Exception) {
                return e.message!!
            } catch (e: OutOfMemoryError) {
                return e.message!!
            }
            return "Success"
        }

        override fun onCancelled() {
            indeterminateBar.visibility = View.INVISIBLE
            answerViewError.visibility = View.INVISIBLE
            differenceText.visibility = View.INVISIBLE
            super.onCancelled()
        }

        override fun onPostExecute(result: String) {

            if (result == "Success") {
                try {
                    // sort and show results
                    setAnswersView()
                    if (!checkShowAnswers.isChecked) {
                        scrollListView.visibility = View.INVISIBLE
                        differenceText.visibility = View.INVISIBLE
                    } else {
                        scrollListView.visibility = View.VISIBLE
                        differenceText.visibility = View.VISIBLE
                    }
                    indeterminateBar.visibility = View.GONE
                    answerViewError.visibility = View.INVISIBLE
                } catch (e: Exception) {
                    setErrors(e.message!!)
                } catch (e: OutOfMemoryError) {
                    setErrors(e.message!!)
                }
            } else {
                setErrors(result)
            }
        }

        fun setErrors(message: String) {
            lookup = mutableMapOf()
            indeterminateBar.visibility = View.GONE
            answerViewError.text = "Sorry, the following error occurred: $message"
            answerViewError.visibility = View.VISIBLE
        }

        override fun onPreExecute() {
            clearAnswersView()
            showIndeterminate()
        }

        override fun onProgressUpdate(vararg values: Void) {}

        private fun compute(equations: ArrayList<Equation>, done: Boolean): ArrayList<Equation> {
            if (isCancelled)
                return arrayListOf()
            if (equations.size == 1) {
                return equations
            } else if (equations.size == 2) {
                try {
                    return if (done) {
                        addToList(computeTwo(equations.elementAt(0), equations.elementAt(1)))
                        arrayListOf()
                    } else {
                        computeTwo(equations.elementAt(0), equations.elementAt(1))
                    }
                }
                catch(e: Exception){
                    var ii = e.message
                }
            }

            var results: ArrayList<Equation> = arrayListOf()

            var half = equations.size / 2 + 1
            //println(half)
            for (index in 1 until half) {
                var c = Combinations(index, equations.size)
                // if we're computing (2,4) then we get back {[0,1], [0,2], [0,3], [1,2], [1,3], [2,3]) and the second half of that list will be computed automatically below
                var upTo = if (equations.size % 2 == 0 && index == equations.size / 2) {
                    c.combos.size / 2
                } else {
                    c.combos.size
                }
                for (i in 0 until upTo) {
                    try {
                        var first: ArrayList<Equation> = arrayListOf()
                        var second: ArrayList<Equation> = arrayListOf()
                        var list = c.combos[i]
                        for (j in 0 until equations.size) {
                            if (list.contains(j)) {
                                first.add(equations.get(j))
                            } else {
                                second.add(equations.get(j))
                            }
                        }
                        var firstList = compute(first, false)
                        var secondList = compute(second, false)

                        for (firstEquation in firstList) {
                            for (secondEquation in secondList) {
                                if (done) {
                                    try {
                                        var res = computeTwo(firstEquation, secondEquation)
                                        addToList(res)
                                    }catch(e : Exception){
                                        var fff = e.message
                                    }
                                } else {
                                    try {
                                        results.addAll(computeTwo(firstEquation, secondEquation))
                                    }catch(e: Exception){
                                        var eeeee = e.message
                                    }
                                }
                            }
                        }
                    }
                    catch(e: Exception){
                        var iis = e.message
                    }
                }
            }
            return results
        }
    }

    /* arr[]  ---> Input Array
    data[] ---> Temporary array to store current combination
    start & end ---> Staring and Ending indexes in arr[]
    index  ---> Current index in data[]
    r ---> Size of a combination to be printed */
    fun combinationUtil(arr: ArrayList<Equation>, data: ArrayList<Equation>, start: Int,
                        end: Int, index: Int, r: Int) : ArrayList<ArrayList<Equation>> {
        // Current combination is ready to be printed, print it
        if (index == r) {
            return arrayListOf(arr, data)
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        var i = start
        while (i <= end && end - i + 1 >= r - index) {
            var copy = arr.clone() as ArrayList<Equation>
            data[index] = copy[i]
            copy.removeAt(i)
            return combinationUtil(copy, data, i + 1, end, index + 1, r)
            i++
        }
        // TODO: this isn't right
        return arrayListOf(arr, data)
    }

    // The main function that prints all combinations of size r
    // in arr[] of size n. This function mainly uses combinationUtil()
    fun printCombination(arr: ArrayList<Equation>, r: Int): ArrayList<ArrayList<Equation>>{
        // A temporary array to store all combination one by one
        val data = arrayListOf<Equation>()

        // Print all combination using temporary array 'data[]'
        return combinationUtil(arr, data, 0, equations.size - 1, 0, r)
    }

    //Receives message from timer to start diceImages roll
    var callback: Handler.Callback = Handler.Callback {
        // reset everything for new computations
        baseline = Double.MAX_VALUE
        equations = ArrayList(10)
        answers = ArrayList(10)
        //nans = ArrayList(10)
        //infinites = ArrayList(10)
        answerListView.adapter = null
        die.clear()

        //Get roll result
        for (i in 0 until numOfDice) {
            var oneDie = rnd.nextInt(numOfSides) + 1
            die.add(Die(oneDie))
            equations.add(SimpleNumber(oneDie))
        }
        target = rnd.nextInt(targetMax) + 1
        targetText!!.text = (target).toString()
        // setAnswers(one, two, three, target)
        rolling = false  //user can press again
        if (hideSolutions) {
            checkShowAnswers.isChecked = false
        }

        setDice()

        mTask = LongOperation()
        mTask.execute()
        true
    }

    private fun setDice(){
        var gLayout: GridLayoutManager
        if (numOfDice % 3 == 0) {
            gLayout = GridLayoutManager(GlobalVariable.context, 3)
        } else if (numOfDice % 2 == 0) {
            gLayout = GridLayoutManager(GlobalVariable.context, 2)
        } else {
            gLayout = GridLayoutManager(GlobalVariable.context, 6)
            gLayout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (position % 5) {
                        0 -> 2
                        1 -> 2
                        2 -> 2
                        3 -> 3
                        4 -> 3
                        else -> -1
                    }
                }
            }
        }
        mLayoutManager = gLayout
        mRecyclerView?.layoutManager = mLayoutManager

        val adapter = mRecyclerView?.adapter as DiceRecyclerViewAdapter
        adapter.setDie(die)
        val callback = ItemMoveCallback(adapter)
        if (numOfDice > 3) {
            callback.setMoreThanOneRow()
        }
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(mRecyclerView)
        mRecyclerView?.adapter = adapter
    }

    private fun setAnswersView() {
        val adapter = TwoColumnListAdapter(answers)
        answerListView.adapter = adapter
        answerListView.layoutManager = LinearLayoutManager(this)
    }
    private fun setBlankAnswersView() {
        val adapter = TwoColumnListAdapter(arrayListOf())
        answerListView.adapter = adapter
        answerListView.layoutManager = LinearLayoutManager(this)
    }

    private fun showIndeterminate(){
        if (checkShowAnswers.isChecked) {
            indeterminateBar.visibility = View.VISIBLE
        }
    }

    private fun clearAnswersView(){
        differenceText.visibility = View.INVISIBLE
        answerViewError.visibility = View.INVISIBLE
        showIndeterminate()
    }

    private fun computeTwo(equation0: Equation, equation1: Equation): ArrayList<Equation> {
        var results = ArrayList<Equation>(10)

        results.add(Add.getOrCreate(equation0, equation1))
        results.add(Subtract.getOrCreate(equation0, equation1))
        results.add(Multiply.getOrCreate(equation0, equation1))
        results.add(Divide.getOrCreate(equation0, equation1))
        if (allowPowers) {
            results.add(PowerOf.getOrCreate(equation0, equation1))
            results.add(RootOf.getOrCreate(equation0, equation1))
        }

        if (equation0.equation != equation1.equation) {
            results.add(Subtract.getOrCreate(equation1, equation0))
            results.add(Divide.getOrCreate(equation1, equation0))
            if (allowPowers) {
                results.add(PowerOf.getOrCreate(equation1, equation0))
                results.add(RootOf.getOrCreate(equation1, equation0))
            }
        }
        return ArrayList(results.filter {
            it.solution != POSITIVE_INFINITY && it.solution != Double.NEGATIVE_INFINITY
                    && it.solution != NaN
        }.distinctBy { it.equation })
    }


    private fun addToList(list: ArrayList<Equation>) {
        // no reason to add anything to the list that is farther away than the current baseline
        // There are still going to be some duplicates, it.equation vs. answers.display -
        // Not sure that can be helped since otherwise we have to store another list or some such
        val cutoffList = list.map { Answer(it, target) }.filter { it.absoluteDiff <= baseline }
        if(cutoffList.isEmpty()){
            return
        }
        else if(cutoffList.size == 1){
            answers.add(cutoffList[0])
        }
        else {
            answers.addAll(cutoffList)
        }
        // take distinct, sort by diff, and then only take the top 500
        var tryList = answers.distinctBy { it.display }.sortedWith(compareBy { it.absoluteDiff }).take(answerListMaxSize)
        if(tryList.isEmpty()){
            answers = arrayListOf()
            return
        }
        else if(tryList.size == 1){
            answers = arrayListOf()
            answers.add(tryList[0])
            return
        }
        answers = tryList as ArrayList<Answer>
        if (answers.size == answerListMaxSize) {
            baseline = answers[answerListMaxSize - 1].absoluteDiff
        }
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

    abstract class Equation : Comparable<Equation> {
        var operator: String = ""
        var equation: String = ""
        var solution: Double = 0.0
        var isAllPlus: Boolean = false
        var isAllTimes: Boolean = false
        var allList: ArrayList<Equation> = arrayListOf()

        constructor(a: Int) {
            solution = a.toDouble()
            equation = a.toString()
            isAllPlus = true
            isAllTimes = true
            allList.add(this)
        }

        constructor(a: Equation, b: Equation, c: String, t: String) {
            this.operator = c
            this.equation = t
            computeSolution(a, b)
            GlobalVariable.lookup[t] = this
        }

        constructor(a: Equation, b: Equation, c: String, t: String, list: ArrayList<Equation>) {
            this.operator = c
            this.equation = t
            this.allList = list
            computeSolution(a, b)
            GlobalVariable.lookup[t] = this
        }

        protected open fun computeSolution(a: Equation, b: Equation) {}

        companion object {
            fun computeText(a: Equation, b: Equation, thisOperator: String): String {
                return if (a is SimpleNumber) {
                    if (b is SimpleNumber) {
                        a.equation + thisOperator + b.equation
                    } else {
                        a.equation + thisOperator + "(" + b.equation + ")"
                    }
                } else {
                    if (b is SimpleNumber) {
                        "(" + a.equation + ")" + thisOperator + b.equation
                    } else {
                        "(" + a.equation + ")" + thisOperator + "(" + b.equation + ")"
                    }
                }
            }
        }

        override operator fun compareTo(other: Equation): Int {
            return this.equation.compareTo(other.equation)
        }
    }

    class SimpleNumber : Equation {
        constructor(a: Int) : super(a)
    }

    class Add : Equation {
        init {
            this.isAllPlus = true
        }

        constructor(a: Equation, b: Equation, t: String, list: ArrayList<Equation>) : super(a, b, "+", t, list)

        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution + b.solution
        }

        companion object {

            fun getOrCreate(a: Equation, b: Equation): Equation {
                var list: ArrayList<Equation> = arrayListOf()
                if (a.isAllPlus) {
                    list.addAll(a.allList)
                } else {
                    list.add(a)
                }
                if (b.isAllPlus) {
                    list.addAll(b.allList)
                } else {
                    list.add(b)
                }
                list.sort()
                var text = list[0].equation
                if (!list[0].isAllPlus) {
                    text = "($text)"
                }
                for (i in 1 until list.size) {
                    text = "$text+"
                    text += if (list[i].isAllPlus) {
                        list[i].equation
                    } else {
                        "(" + list[i].equation + ")"
                    }
                }
                val eq = GlobalVariable.lookup.get(text)
                if (eq != null) {
                    return eq
                }
                var add = Add(a, b, text, list)
                return add
            }
        }
    }

    class Subtract : Equation {
        init {
        }
        constructor(a: Equation, b: Equation, t: String) : super(a, b, "-", t)

        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution - b.solution
        }

        companion object {
            fun computeText(a: Equation, b: Equation): String {
                return Equation.computeText(a, b, "-")
            }

            fun getOrCreate(a: Equation, b: Equation): Equation {
                val t = Subtract.computeText(a, b)
                val eq = GlobalVariable.lookup.get(t)
                if (eq != null) {
                    return eq
                }
                return Subtract(a, b, t)
            }
        }
    }

    class Multiply : Equation {
        init {
            this.isAllTimes = true
        }

        constructor(a: Equation, b: Equation, t: String, list: ArrayList<Equation>) : super(a, b, "*", t, list)

        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution * b.solution
        }

        companion object {
            fun getOrCreate(a: Equation, b: Equation): Equation {
                var list: ArrayList<Equation> = arrayListOf()
                if (a.isAllTimes) {
                    list.addAll(a.allList)
                } else {
                    list.add(a)
                }
                if (b.isAllTimes) {
                    list.addAll(b.allList)
                } else {
                    list.add(b)
                }
                list.sort()
                var text = list[0].equation
                if (!list[0].isAllTimes) {
                    text = "($text)"
                }
                for (i in 1 until list.size) {
                    text = "$text*"
                    text += if (list[i].isAllTimes) {
                        list[i].equation
                    } else {
                        "(" + list[i].equation + ")"
                    }
                }
                val eq = GlobalVariable.lookup.get(text)
                if (eq != null) {
                    return eq
                }
                var add = Multiply(a, b, text, list)
                return add
            }
        }
    }

    class Divide : Equation {
        init {
        }
        constructor(a: Equation, b: Equation, t: String) : super(a, b, "/", t)

        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution / b.solution
        }

        companion object {
            fun computeText(a: Equation, b: Equation): String {
                return Equation.computeText(a, b, "/")
            }

            fun getOrCreate(a: Equation, b: Equation): Equation {
                val t = Divide.computeText(a, b)
                val eq = GlobalVariable.lookup.get(t)
                if (eq != null) {
                    return eq
                }
                return Divide(a, b, t)
            }
        }
    }

    class PowerOf : Equation {
        init {
        }
        constructor(a: Equation, b: Equation, t: String) : super(a, b, "^", t)

        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution.pow(b.solution)
        }

        companion object {
            fun computeText(a: Equation, b: Equation): String {
                return if (a is SimpleNumber) {
                    if (b is SimpleNumber) {
                        a.equation + "<sup>" + b.equation + "</sup>"
                    } else {
                        a.equation + "<sup>" + "(" + b.equation + ")" + "</sup>"
                    }
                } else {
                    if (b is SimpleNumber) {
                        "(" + a.equation + ")" + "<sup>" + b.equation + "</sup>"
                    } else {
                        "(" + a.equation + ")" + "<sup>" + "(" + b.equation + ")" + "</sup>"
                    }
                }
            }

            fun getOrCreate(a: Equation, b: Equation): Equation {
                val t = PowerOf.computeText(a, b)
                val eq = GlobalVariable.lookup.get(t)
                if (eq != null) {
                    return eq
                }
                return PowerOf(a, b, t)
            }
        }
    }

    class RootOf : Equation {
        init {
        }
        constructor(a: Equation, b: Equation, t: String) : super(a, b, "~", t)

        override fun computeSolution(a: Equation, b: Equation) {
            solution = a.solution.pow(1 / b.solution)
        }

        companion object {
            fun computeText(a: Equation, b: Equation): String {
                return if (a is SimpleNumber) {
                    if (b is SimpleNumber) {
                        a.equation + "<sup>1/" + b.equation + "</sup>"
                    } else {
                        a.equation + "<sup>1/" + "(" + b.equation + ")" + "</sup>"
                    }
                } else {
                    if (b is SimpleNumber) {
                        "(" + a.equation + ")" + "<sup>1/" + b.equation + "</sup>"
                    } else {
                        "(" + a.equation + ")" + "<sup>1/" + "(" + b.equation + ")" + "</sup>"
                    }
                }
            }

            fun getOrCreate(a: Equation, b: Equation): Equation {
                val t = RootOf.computeText(a, b)
                val eq = GlobalVariable.lookup.get(t)
                if (eq != null) {
                    return eq
                }
                return RootOf(a, b, t)
            }
        }
    }

    inner class Answer {
        var display: String = ""
        var absoluteDiff: Double = 0.0
        var absoluteDiffDisplay: String = ""
        var plusMinus: Int = -1

        constructor(eq: Equation, target: Int) {
            // not sure if it's worth this to try to filter out duplicates first, or not
            //equation = eq.equation
            display = display(eq)
            var diff = eq.solution - target
            absoluteDiff = Math.abs(diff)
            if (diff != 0.0) {
                absoluteDiffDisplay = tryRoundSolution(absoluteDiff)
                if (absoluteDiff < 0.01) {
                    plusMinus = 0
                } else if (diff > 0) {
                    plusMinus = 1
                }
            } else {
                absoluteDiffDisplay = "0"
                plusMinus = 0
            }
        }

        fun display(eq: Equation) = eq.equation + " = " + tryRoundSolution(eq.solution)

        private fun tryRoundSolution(sol: Double): String {
            if (sol.isNaN() || sol.isInfinite()) {
                return sol.toString()
            }
            var rounded = sol.roundToInt()
            if (sol - rounded != 0.0) {
                return "%.2f".format(sol)
            }
            return rounded.toString()
        }
    }

    fun <Int> permute(list: List<Int>): List<List<Int>> {
        if (list.size == 1) return listOf(list)
        val perms = mutableListOf<List<Int>>()
        val sub = list[0]
        for (perm in permute(list.drop(1)))
            for (i in 0..perm.size) {
                val newPerm = perm.toMutableList()
                newPerm.add(i, sub)
                perms.add(newPerm)
            }
        return perms.distinct()
    }
    override fun onNoteListChanged(l: List<Die>)
    {

    }
}
interface OnDiceListChangedListener {
    fun onNoteListChanged(dice: List<Die>)
}

interface OnStartDragListener {
    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}

// TODO: return method from class not init new class every time
class Combinations(val m: Int, val n: Int) {
    public var combos: MutableList<IntArray> = mutableListOf()
    private val combination = IntArray(m)
    private val key = "${m},${n}"

    init {
        if(Roll.GlobalVariable.combinations.containsKey(key)){
            combos = Roll.GlobalVariable.combinations.get(key) as MutableList<IntArray>
        }
        else {
            generate(0)
        }
    }

    private fun generate(k: Int) {
        if (k >= m) {
            var copy = IntArray(m)
            for (i in 0 until m){
                copy[i] = combination[i]
            }
            combos.add(copy)
        }
        else {
            for (j in 0 until n) {
                if (k == 0 || j > combination[k - 1]) {
                    combination[k] = j
                    generate(k + 1)
                }
            }
        }
    }
}