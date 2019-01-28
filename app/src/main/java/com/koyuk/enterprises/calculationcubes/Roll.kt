package com.koyuk.enterprises.calculationcubes

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.roll.*
import android.os.*
import android.support.v7.app.AlertDialog
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
import java.util.*
import android.util.Log
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.koyuk.enterprises.calculationcubes.com.koyuk.enterprises.calculationcubes.models.Answer
import com.koyuk.enterprises.calculationcubes.com.koyuk.enterprises.calculationcubes.models.Equation
import com.koyuk.enterprises.calculationcubes.com.koyuk.enterprises.calculationcubes.models.SimpleNumber
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Dice code adapted from https://tekeye.uk/android/examples/android-diceImages-code
 *
 **/
class Roll : AppCompatActivity(), CoroutineScope, OnStartDragListener, OnDiceListChangedListener {

    var prefs: Prefs? = null

    private val answerListMaxSize = 500

    lateinit var die: MutableList<Die>
    //lateinit var infinites: ArrayList<Answer>
    //lateinit var nans: ArrayList<Answer>
    lateinit var settings: View
    lateinit var upDownButton: MenuItem

    var rnd = Random()
    var rolling = false
    var isUp = true
    var hasAnswers = false

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: DiceRecyclerViewAdapter? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mItemTouchHelper: ItemTouchHelper? = null

    lateinit var emptyDieArray: MutableList<Die>

    var rolls = 0
    var rollsBeforeAds = 8
    private lateinit var mInterstitialAd: InterstitialAd

    object GlobalVariable {
        var context: Context? = null
    }

    lateinit var job: Job
    lateinit var calculateJob: Job
    @Volatile
    var calculating = false
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalVariable.context = this
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        job = Job()
        calculateJob = Job()


        setContentView(R.layout.roll)
        setSupportActionBar(findViewById(R.id.toolbar))
        //getSupportActionBar()!!.setDisplayShowTitleEnabled(false);

        //Our function to initialise sound playing
        rollButton!!.setOnClickListener(HandleClick())
        die = mutableListOf()

        setupRecyclerView()

        hideSolutionsCB.isChecked = prefs!!.hideSolutions
        checkShowAnswers.isChecked = prefs!!.solutionsChecked

        checkShowAnswers.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                scrollListView.visibility = View.VISIBLE
                if(calculating) {
                    // My AsyncTask is currently doing work in doInBackground()
                    indeterminateBar.visibility = View.VISIBLE
                    differenceText.visibility = View.INVISIBLE
                }
                else if (hasAnswers) {
                    differenceText.visibility = View.VISIBLE
                }
                else{
                    differenceText.visibility = View.INVISIBLE
                }
            } else {
                scrollListView.visibility = View.INVISIBLE
                differenceText.visibility = View.INVISIBLE
            }
            prefs!!.solutionsChecked = isChecked
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
        spinner.setSelection(spinnerdapter.getPosition(prefs!!.numOfDice.toString()))


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
        spinnerSides.setSelection(spinneradapterSides.getPosition(prefs!!.numOfSides.toString()))

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
        if (prefs!!.numOfDice % 3 == 0) {
            gLayout = GridLayoutManager(this, 3)
        } else if (prefs!!.numOfDice % 2 == 0) {
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
        for (i in 0 until prefs!!.numOfDice) {
            emptyDieArray.add(Die(0))
        }
        maxTargetText.text = "/" + prefs!!.targetMax

        return emptyDieArray
    }

    private fun Context.toast(message: CharSequence) =
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun setSettings() {
        prefs!!.hideSolutions = hideSolutionsCB.isChecked
        if (prefs!!.hideSolutions) {
            checkShowAnswers.isChecked = false
        }
        val maxTarget = findViewById<EditText>(R.id.setMaxTarget).text.toString()
        try {
            val tempTargetMax = Integer.parseInt(maxTarget)
            if (tempTargetMax > 9999) {
                toast("Max Target must be less than 9999")
                return
            }
            prefs!!.targetMax = tempTargetMax
            maxTargetText.text = "/" + prefs!!.targetMax
        } catch (e: Exception) {
            toast("Please enter a valid integer")
            return
        }
        val prevAllowPowers = prefs!!.allowPowers
        prefs!!.allowPowers = allowPowersCB.isChecked

        var spinner = findViewById<Spinner>(R.id.numDiceSpinner)
        val prevNumberDice = prefs!!.numOfDice
        prefs!!.numOfDice = spinner.selectedItemPosition + 2

        if (prevNumberDice != prefs!!.numOfDice) {
            if(calculateJob.isActive) {
                indeterminateBar.visibility = View.INVISIBLE
                calculateJob.cancel()
            }
            val answers: ArrayList<Answer> = arrayListOf()
            setAnswersView(answers)
            targetText.text = ""
            die.clear()
            die = getEmptyDie()
            setDice()
        } else {
            if (prevAllowPowers != prefs!!.allowPowers) {
                setBlankAnswersView()
                computeAnswers()
            }
        }
        var spinnerSides = findViewById<Spinner>(R.id.numDiceSidesSpinner)
        prefs!!.numOfSides = spinnerSides.selectedItemPosition + 2

        slideUp()
    }

    fun slideUp() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0)
        } catch (e: Exception) {

        }
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

    private fun showCurrentSettings() {
        var max = findViewById<EditText>(R.id.setMaxTarget)
        max.setText(prefs!!.targetMax.toString())
        numDiceSpinner.setSelection(prefs!!.numOfDice - 2)
        numDiceSidesSpinner.setSelection(prefs!!.numOfSides - 2)
        allowPowersCB.isChecked = prefs!!.allowPowers
        hideSolutionsCB.isChecked = prefs!!.hideSolutions
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
            if (!rolling) {
                rolling = true
               computeAnswers()
            }
        }
    }

    // The main function that prints all combinations of size r
    // in arr[] of size n. This function mainly uses combinationUtil()
/*    fun printCombination(arr: ArrayList<Equation>, r: Int): ArrayList<ArrayList<Equation>>{
        // A temporary array to store all combination one by one
        val data = arrayListOf<Equation>()

        // Print all combination using temporary array 'data[]'
        return combinationUtil(arr, data, 0, equations.size - 1, 0, r)
    }*/

    fun computeAnswers() {
        calculating = true
        if (calculateJob.isActive) {
            calculateJob.cancel()
        }
        //Receives message from timer to start diceImages roll
        // reset everything for new computations
        var equations: ArrayList<Equation> = arrayListOf()
        answerListView.adapter = null
        die.clear()

        clearAnswersView()
        showIndeterminate()

        //Get roll result
        for (i in 0 until prefs!!.numOfDice) {
            var oneDie = rnd.nextInt(prefs!!.numOfSides) + 1
            die.add(Die(oneDie))
            equations.add(SimpleNumber(oneDie))
        }
        var target = rnd.nextInt(prefs!!.targetMax) + 1
        targetText!!.text = (target).toString()
        // setAnswers(one, two, three, target)
        rolling = false  //user can press again
        if (prefs!!.hideSolutions) {
            checkShowAnswers.isChecked = false
        }

        setDice()

        var answers: ArrayList<Answer> = arrayListOf()
        launch {
            calculateJob = GlobalScope.launch(Dispatchers.IO)
            {
                var calculate = Calculate(answerListMaxSize, prefs!!.allowPowers, target)
                val first = async { calculate.computeAnswers(equations) }
                answers = first.await()
            }
            calculateJob.join()

            if(!calculateJob.isCancelled && answers.size > 0) {
                setAnswers(answers)
                calculating = false
            }
        }
    }

    private fun setAnswers(answers: ArrayList<Answer>){
        println("In setAnswers")
        try {
            // sort and show results
            setAnswersView(answers)
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

    }

    private fun setDice(){
        var gLayout: GridLayoutManager
        if (prefs!!.numOfDice % 3 == 0) {
            gLayout = GridLayoutManager(GlobalVariable.context, 3)
        } else if (prefs!!.numOfDice % 2 == 0) {
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
        if (prefs!!.numOfDice > 3) {
            callback.setMoreThanOneRow()
        }
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(mRecyclerView)
        mRecyclerView?.adapter = adapter
    }
    private fun setAnswersView(answers: ArrayList<Answer>) {
        hasAnswers = answers.size > 0
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
    fun setErrors(message: String) {
        indeterminateBar.visibility = View.GONE
        answerViewError.text = "Sorry, the following error occurred: $message"
        answerViewError.visibility = View.VISIBLE
    }

    //Clean up
    override fun onPause() {
        super.onPause()
//        diceSound!!.pause(soundId)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        //timer.cancel()
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