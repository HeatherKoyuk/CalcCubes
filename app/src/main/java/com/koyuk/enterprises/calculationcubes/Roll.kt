package com.koyuk.enterprises.calculationcubes

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
// import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
//import com.koyuk.enterprises.calculationcubes.BillingManager.GlobalVariable.hasPro
import com.koyuk.enterprises.calculationcubes.models.Answer
import com.koyuk.enterprises.calculationcubes.models.Equation
import com.koyuk.enterprises.calculationcubes.models.SimpleNumber
import com.koyuk.enterprises.calculationcubes.popups.*
import kotlinx.android.synthetic.main.roll.*
import kotlinx.android.synthetic.main.settings.*
import kotlinx.coroutines.*
import java.lang.Integer.parseInt
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Dice code adapted from https://tekeye.uk/android/examples/android-diceImages-code
 *
 **/
class Roll : AppCompatActivity(), CoroutineScope, OnStartDragListener, OnDiceListChangedListener, RewardedVideoAdListener {

    var prefs: Prefs? = null

    private val answerListMaxSize = 500

    private lateinit var die: MutableList<Die>
    //lateinit var infinites: ArrayList<Answer>
    //lateinit var nans: ArrayList<Answer>
    private lateinit var settings: View
    private lateinit var upDownButton: MenuItem

    private var rnd = Random()
    var rolling = false
    private var isUp = true
    private var hasAnswers = false
    private var target = 0

    private val numberRegex = Regex("[0-9]*")

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: DiceRecyclerViewAdapter? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mItemTouchHelper: ItemTouchHelper? = null

    private lateinit var emptyDieArray: MutableList<Die>

    var rolls = 0
    val rollsBeforeAds = 8
    var adsDisplayed = 3
    val adsBeforeDisplayVideoOption = 5
    val HOUR = 3600*1000

    // Late initialize an alert dialog object
    private lateinit var dialog: AlertDialog
    private lateinit var mInterstitialAd: InterstitialAd
    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private lateinit var extras: Bundle

    private var isTest = false
    val interstitialTestAdId = "ca-app-pub-3940256099942544/1033173712"
    val interstitialReleaseAdId = "ca-app-pub-4140639688578770/2023851391"
    val videoTestAdId = "ca-app-pub-3940256099942544/5224354917"
    val videoReleaseAdId = "ca-app-pub-4140639688578770/8774834561"

    // private lateinit var binding: RollBinding

    object GlobalVariable {
        var context: Context? = null
    }

    private lateinit var job: Job
    private lateinit var calculateJob: Job
    @Volatile
    var calculating = false
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalVariable.context = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = Prefs(this)
        job = Job()
        calculateJob = Job()

        setContentView(R.layout.roll)
        setSupportActionBar(findViewById(R.id.toolbar))

        //Our function to initialise sound playing
        rollButton!!.setOnClickListener(HandleClick())
        die = getEmptyDie()
        setupRecyclerView()

        hideSolutionsCB.isChecked = prefs!!.hideSolutions
        checkShowAnswers.isChecked = prefs!!.solutionsChecked

        checkShowAnswers.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                scrollListView.visibility = View.VISIBLE
                when {
                    calculating -> {
                        // My AsyncTask is currently doing work in doInBackground()
                        indeterminateBar.visibility = View.VISIBLE
                        differenceText.visibility = View.INVISIBLE
                    }
                    hasAnswers -> differenceText.visibility = View.VISIBLE
                    else -> differenceText.visibility = View.INVISIBLE
                }
            } else {
                scrollListView.visibility = View.INVISIBLE
                differenceText.visibility = View.INVISIBLE
            }
            prefs!!.solutionsChecked = isChecked
        }
        settings = findViewById(R.id.calcSettingsView)
        settings.bringToFront()

        val spinner = findViewById<Spinner>(R.id.numDiceSpinner)
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


        val spinnerSides = findViewById<Spinner>(R.id.numDiceSidesSpinner)
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

        val spinnerMultiply = findViewById<Spinner>(R.id.multiplyNumDice)
        val spinnerdapterMultiply = ArrayAdapter.createFromResource(this, R.array.threeArray, R.layout.spinner_text)
        spinnerdapterMultiply.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerMultiply.adapter = spinnerdapterMultiply

        spinnerMultiply.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                parent.getItemAtPosition(position)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        spinnerMultiply.setSelection(spinnerdapterMultiply.getPosition(prefs!!.multiplyNumDice.toString()))

        val spinnerMultiplySides = findViewById<Spinner>(R.id.multiplyDiceSides)
        val spinnerdapterMultiplySides = ArrayAdapter.createFromResource(this, R.array.numDiceSidesArray, R.layout.spinner_text)
        spinnerdapterMultiplySides.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerMultiplySides.adapter = spinnerdapterMultiplySides

        spinnerMultiplySides.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                parent.getItemAtPosition(position)
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }
        spinnerMultiplySides.setSelection(spinnerdapterMultiplySides.getPosition(prefs!!.multiplyDiceSides.toString()))

        homeButton.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        val submitButton = findViewById<Button>(R.id.submitSettingsButton)
        submitButton.setOnClickListener {
            setSettings()
        }
        val cancelButton = findViewById<Button>(R.id.settingsCancelButton)
        cancelButton.setOnClickListener {
            slideUp()
        }

     //   if(!hasPro) {
            extras = Bundle()
            extras.putString("max_ad_content_rating", "G")

            MobileAds.initialize(this, "ca-app-pub-4140639688578770~6032762053")

            mInterstitialAd = InterstitialAd(this)
            if (isTest) {
                mInterstitialAd.adUnitId = interstitialTestAdId
            } else {
                mInterstitialAd.adUnitId = interstitialReleaseAdId
            }
            loadInterstitialAd()

            mInterstitialAd.adListener = object : AdListener() {
                override fun onAdClosed() {
                    loadInterstitialAd()
                }
            }
            mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
            mRewardedVideoAd.rewardedVideoAdListener = this;
            loadRewardedVideoAd()

            // Initialize a new instance of alert dialog builder object
            val builder = AlertDialog.Builder(this)

            // Set a title for alert dialog
            builder.setTitle("")

            // Set a message for alert dialog
            builder.setMessage("Do you want to watch a short video to opt out of ads for 2 hours?")

            // On click listener for dialog buttons
            val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> if (mRewardedVideoAd.isLoaded) {
                        mRewardedVideoAd.show()
                    }
                    DialogInterface.BUTTON_NEGATIVE -> if (mInterstitialAd.isLoaded) {
                        adsDisplayed += 1
                        mInterstitialAd.show()
                    }
                    DialogInterface.BUTTON_NEUTRAL -> blockPopup()
                }
            }

            // Set the alert dialog positive/yes button
            builder.setPositiveButton("YES", dialogClickListener)

            // Set the alert dialog negative/no button
            builder.setNegativeButton("NO", dialogClickListener)

            // Set the alert dialog negative/no button
            builder.setNeutralButton("No, and Do not offer again for 24h", dialogClickListener)

            // Initialize the AlertDialog using builder object
            dialog = builder.create()
     //   }

        targetTextEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (targetTextEdit.visibility == View.VISIBLE) {
                    clearAnswers()
                }
            }
        })

        or.setTypeface(null, Typeface.ITALIC)

        ckMaxTarget.isChecked = prefs!!.useCustomMaxTarget
        ckMaxTarget.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                ckMaxTargetDie.isChecked = false
            }
        }
        ckMaxTargetDie.isChecked = !(prefs!!.useCustomMaxTarget)
        ckMaxTargetDie.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                ckMaxTarget.isChecked = false
            }
        }

        if (prefs!!.target > 0) {
            target = prefs!!.target
            targetText.text = prefs!!.target.toString()
            targetTextEdit.setText(prefs!!.target.toString())
        }
        die.clear()
        for (i in 0 until prefs!!.numOfDice) {
            die.add(Die(prefs!!.getDie(i)))
        }
        setEditMode(prefs!!.editMode)

        if (prefs!!.allDieSet()) {
            computeAnswers(checkShowAnswers.isChecked)
        }

        // levels
        ckAddSubtract.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                ckMultiplyDivide.isChecked = false
                ckPowersRoots.isChecked = false
            }
        }
        ckMultiplyDivide.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                ckAddSubtract.isChecked = false
                ckPowersRoots.isChecked = false
            }
        }
        ckPowersRoots.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                ckAddSubtract.isChecked = false
                ckMultiplyDivide.isChecked = false
            }
        }

        // set info popups
        infoEditModeButton.setOnClickListener {
            val intent = Intent(this, EditMode::class.java)
            startActivity(intent)
        }
        infoNumDiceButton.setOnClickListener {
            val intent = Intent(this, NumberDice::class.java)
            startActivity(intent)
        }
        infoNumSidesButton.setOnClickListener {
            val intent = Intent(this, NumberSides::class.java)
            startActivity(intent)
        }
        infoMaxTargetButton.setOnClickListener {
            val intent = Intent(this, TargetOptions::class.java)
            startActivity(intent)
        }
        infoLevelButton.setOnClickListener {
            val intent = Intent(this, PowersRoots::class.java)
            startActivity(intent)
        }
        infoHideSolutionsButton.setOnClickListener {
            val intent = Intent(this, HideSolutions::class.java)
            startActivity(intent)
        }
    }

    private fun blockPopup(){
        var currentDateTime = Date()
        prefs!!.blockPopupEndTime = Date(currentDateTime.time + 24 * HOUR)
        if (mInterstitialAd.isLoaded) {
            adsDisplayed += 1
            mInterstitialAd.show()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            hideKeyboard()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        upDownButton = menu.findItem(R.id.settings_menu)
        return true
    }

    private fun setupRecyclerView() {
        mRecyclerView = findViewById(R.id.diceView)
        mRecyclerView?.setHasFixedSize(true)
        val gLayout: GridLayoutManager
        when {
            prefs!!.numOfDice % 3 == 0 -> gLayout = GridLayoutManager(this, 3)
            prefs!!.numOfDice % 2 == 0 -> gLayout = GridLayoutManager(this, 2)
            else -> {
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
        }
        mLayoutManager = gLayout
        mRecyclerView?.layoutManager = mLayoutManager

        //setup the adapter with empty list
        mAdapter = DiceRecyclerViewAdapter(this, getEmptyDie(), this, this, this, prefs!!.editMode)
        val callback = ItemMoveCallback(mAdapter!!)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(mRecyclerView)

        mRecyclerView?.adapter = mAdapter
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper?.startDrag(viewHolder)
    }

    private fun getEmptyDie(): MutableList<Die> {
        differenceText.visibility = View.INVISIBLE

        emptyDieArray = mutableListOf()
        for (i in 0 until prefs!!.numOfDice) {
            emptyDieArray.add(Die(0))
        }
        if (prefs!!.useCustomMaxTarget) {
            maxTargetText.text = "/" + prefs!!.manualMaxTarget
        } else {
            maxTargetText.text = "/(" + prefs!!.multiplyNumDice + ")*(" + prefs!!.multiplyDiceSides + ")"
        }

        return emptyDieArray
    }

    private fun toast(message: CharSequence) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP, 0, 400)
        toast.show()
    }

    private fun setSettings() {
        prefs!!.hideSolutions = hideSolutionsCB.isChecked

        prefs!!.multiplyNumDice = multiplyNumDice.selectedItemPosition + 1
        prefs!!.multiplyDiceSides = multiplyDiceSides.selectedItemPosition + 2
        prefs!!.useCustomMaxTarget = ckMaxTarget.isChecked

        val manualMaxTarget = findViewById<EditText>(R.id.setMaxTarget).text.toString()
        try {
            val tempTargetMax = Integer.parseInt(manualMaxTarget)
            when {
                tempTargetMax > 9999 -> {
                    toast("Max Target must be less than 9999")
                    return
                }
                tempTargetMax < 1 -> {
                    toast("Max Target must be greater than 1")
                    return
                }
                else -> {
                    prefs!!.manualMaxTarget = tempTargetMax
                    maxTargetText.text = "/$tempTargetMax"
                }
            }
        } catch (e: Exception) {
            if (prefs!!.useCustomMaxTarget) {
                toast("Please enter a valid integer for Max Target")
            }
            // ignore if using dice
        }

        if (prefs!!.useCustomMaxTarget) {
            maxTargetText.text = "/" + prefs!!.manualMaxTarget
        } else {
            maxTargetText.text = "/(" + prefs!!.multiplyNumDice + ")*(" + prefs!!.multiplyDiceSides + ")"
        }


        val prevLevel = prefs!!.level
        if(ckAddSubtract.isChecked) {
            prefs!!.level = 0
        }
        else if(ckMultiplyDivide.isChecked){
            prefs!!.level = 1
        }
        else{
            prefs!!.level = 2
        }

        val prevEditMode = prefs!!.editMode
        prefs!!.editMode = editModeToggle.isChecked

        val spinner = findViewById<Spinner>(R.id.numDiceSpinner)
        val prevNumberDice = prefs!!.numOfDice
        prefs!!.numOfDice = spinner.selectedItemPosition + 2

        if (prevNumberDice != prefs!!.numOfDice) {
            clearAnswers()
            target = 0
            prefs!!.target = target
            targetText.text = ""
            targetTextEdit.setText("")
            die.clear()
            die = getEmptyDie()
            setDice(true)
            setEditMode(prefs!!.editMode)

            if(prefs!!.numOfDice > 4){
                toast("Note: 5 dice may take 60 seconds or longer to compute")
            }
        } else {
            if (prevEditMode != prefs!!.editMode) {
                setEditMode(prefs!!.editMode)
            }
            if (prevLevel != prefs!!.level) {
                setBlankAnswersView()
                computeAnswers(checkShowAnswers.isChecked)
            }
        }
        val spinnerSides = findViewById<Spinner>(R.id.numDiceSidesSpinner)
        prefs!!.numOfSides = spinnerSides.selectedItemPosition + 2

        slideUp()
    }

    fun clearAnswers() {
        if (calculateJob.isActive) {
            indeterminateBar.visibility = View.INVISIBLE
            calculateJob.cancel()
        }
        differenceText.visibility = View.INVISIBLE
        val answers: ArrayList<Answer> = arrayListOf()
        setAnswersView(answers)
    }

    private fun setEditMode(editMode: Boolean) {
        mAdapter!!.setEdit(editMode)
        setDice()
        if (editMode) {
            if (target != 0) {
                targetText.text = target.toString()
                targetTextEdit.setText(target.toString())
            } else {
                targetText.text = ""
                targetTextEdit.setText("")
            }
            targetText.visibility = View.GONE
            targetTextEdit.visibility = View.VISIBLE
            maxTargetText.visibility = View.GONE
            rollButton.text = "Compute!"
        } else {
            targetText.visibility = View.VISIBLE
            targetTextEdit.visibility = View.GONE
            if (target != 0) {
                targetText.text = target.toString()
                targetTextEdit.setText(target.toString())
            } else {
                targetText.text = ""
                targetTextEdit.setText("")
            }
            maxTargetText.visibility = View.VISIBLE
            rollButton.text = "Roll!"
        }
    }

    private fun slideUp() {
        hideKeyboard()
        settings.animate()
                .translationY(0f)
                .setDuration(500)
                .start()
        upDownButton.setIcon(R.drawable.baseline_keyboard_arrow_down_grey_18dp)
        isUp = !isUp

        val inout = AnimationUtils.makeInAnimation(this, false)
        rollButton.startAnimation(inout)
        rollButton.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        } catch (e: Exception) {

        }
    }

    // slide the view from its current position to below itself
    private fun slideDown() {
        showCurrentSettings()
        settings.animate()
                .translationY(calcSettingsView.height.toFloat())
                .setDuration(500)
                .start()
        upDownButton.setIcon(R.drawable.baseline_keyboard_arrow_up_grey_18dp)
        isUp = !isUp

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val out = AnimationUtils.makeOutAnimation(this, true)
        rollButton.startAnimation(out)
        rollButton.visibility = View.INVISIBLE
    }

    fun onSlideSettingsView(button: MenuItem) {
        if (isUp) {
            slideDown()
        } else {
            slideUp()
        }
    }

    private fun showCurrentSettings() {
        ckAddSubtract.isChecked = false
        ckMultiplyDivide.isChecked = false
        ckPowersRoots.isChecked = false
        setMaxTarget.setText(prefs!!.manualMaxTarget.toString())
        multiplyNumDice.setSelection(prefs!!.multiplyNumDice - 1)
        multiplyDiceSides.setSelection(prefs!!.multiplyDiceSides - 2)
        numDiceSpinner.setSelection(prefs!!.numOfDice - 2)
        numDiceSidesSpinner.setSelection(prefs!!.numOfSides - 2)
        when {
            prefs!!.level == 0 -> ckAddSubtract.isChecked = true
            prefs!!.level == 1 -> ckMultiplyDivide.isChecked = true
            else -> ckPowersRoots.isChecked = true
        }
        hideSolutionsCB.isChecked = prefs!!.hideSolutions
        editModeToggle.isChecked = prefs!!.editMode
        ckMaxTarget.isChecked = prefs!!.useCustomMaxTarget
        ckMaxTargetDie.isChecked = !prefs!!.useCustomMaxTarget
    }

    //User pressed diceImages, lets start
    private inner class HandleClick : View.OnClickListener {
        override fun onClick(arg0: View) {
            var date = Date()
            //if(!hasPro){
                if(prefs!!.blockAdsEndTime < Date()) {
                    if (rolls >= rollsBeforeAds) {
                        rolls = 0
                        when {
                            prefs!!.blockPopupEndTime < date && adsDisplayed >= adsBeforeDisplayVideoOption -> {
                                adsDisplayed = 0
                                showDialog()
                            }
                            mInterstitialAd.isLoaded -> {
                                if (prefs!!.blockPopupEndTime < date) {
                                    adsDisplayed += 1
                                }
                                mInterstitialAd.show()
                            }
                            // else -> Log.d("TAG", "The interstitial wasn't loaded yet.")
                        }
                    } else {
                        rolls++
                    }
                }
            //}
            if (!rolling) {
                // TODO: fix this - rolling
                //              rolling = true
                if (prefs!!.editMode) {
                    computeAnswers()
                } else {
                    rollAndComputeAnswers()
                }
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

    private fun showDialog(){
        // Finally, display the alert dialog
        dialog.show()
    }

    fun rollAndComputeAnswers() {
        calculating = true
        if (calculateJob.isActive) {
            calculateJob.cancel()
        }
        //Receives message from timer to start diceImages roll
        // reset everything for new computations
        val equations: ArrayList<Equation> = arrayListOf()
        answerListView.adapter = null
        die.clear()

        clearAnswersView()
        showIndeterminate()

        //Get roll result
        for (i in 0 until prefs!!.numOfDice) {
            val oneDie = rnd.nextInt(prefs!!.numOfSides) + 1
            die.add(Die(oneDie))
            equations.add(SimpleNumber(oneDie))
        }
        if (prefs!!.useCustomMaxTarget) {
            target = rnd.nextInt(prefs!!.manualMaxTarget) + 1
        } else {
            target = 1
            for (i in 0 until prefs!!.multiplyNumDice) {
                target *= (rnd.nextInt(prefs!!.multiplyDiceSides) + 1)
            }
        }
        targetText!!.text = (target).toString()
        prefs!!.target = target
        // setAnswers(one, two, three, target)
        rolling = false  //user can press again
        if (prefs!!.hideSolutions) {
            checkShowAnswers.isChecked = false
        }

        setDice()
        calculateAnswers(equations)
    }

    fun computeAnswers(dontHideSolutions: Boolean = false) {
        val values = mAdapter!!.retrieveData()
        val dieArray: ArrayList<Int> = arrayListOf()
        for (value in values) {
            if (value.value == null || value.value!!.trim().isEmpty()) {
                toast("Enter valid numbers for all dice")
                return
            }
            val stringValue = value.value!!.trim()

            if (!stringValue.matches(numberRegex)) {
                toast("Enter valid numbers for all dice")
                return
            }
            try {
                val intValue = parseInt(stringValue)
                if (intValue <= 0 || intValue > 12) {
                    toast("Dice values must be between 1 and 12")
                    return
                }
                dieArray.add(intValue)
            } catch (e: Exception) {
                toast("Enter valid numbers for all dice")
                return
            }
        }
        var stringTarget = ""
        stringTarget = if (prefs!!.editMode) {
            targetTextEdit.text.toString()
        } else {
            target.toString()
        }
        var newTarget = 0
        if (stringTarget == null || stringTarget.trim().isEmpty()) {
            toast("Enter valid number for target number")
            return
        }
        if (!stringTarget.matches(numberRegex)) {
            toast("Enter valid number for target number")
            return
        }
        try {
            newTarget = parseInt(stringTarget)
        } catch (e: Exception) {
            toast("Enter valid number for target number")
            return
        }
        if (newTarget <= 0 || newTarget > 9999) {
            toast("Target must be between 1 and 9999")
            return
        }

        calculating = true
        if (calculateJob.isActive) {
            calculateJob.cancel()
        }
        //Receives message from timer to start diceImages roll
        // reset everything for new computations
        val equations: ArrayList<Equation> = arrayListOf()
        answerListView.adapter = null
        die.clear()

        hideKeyboard()

        clearAnswersView()
        showIndeterminate()

        //Get roll result
        for (i in 0 until dieArray.size) {
            val oneDie = dieArray[i]
            die.add(Die(oneDie))
            equations.add(SimpleNumber(oneDie))
        }
        target = newTarget
        prefs!!.target = target
        targetText!!.text = (target).toString()
        // setAnswers(one, two, three, target)
        rolling = false  //user can press again
        if (prefs!!.hideSolutions && !dontHideSolutions) {
            checkShowAnswers.isChecked = false
        }
        setDice()
        calculateAnswers(equations)
    }

    private fun calculateAnswers(equations: ArrayList<Equation>) {
        var answers: ArrayList<Answer> = arrayListOf()
        launch {
            calculateJob = GlobalScope.launch(Dispatchers.IO)
            {
                val calculate = Calculate(answerListMaxSize, prefs!!.level, target)
                val first = async { calculate.computeAnswers(equations) }
                answers = first.await()
            }
            calculateJob.join()

            if (!calculateJob.isCancelled && answers.size > 0) {
                setAnswers(answers)
                calculating = false
            }
        }
    }

    private fun setAnswers(answers: ArrayList<Answer>) {
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

    private fun setDice(clear: Boolean = false) {
        val gLayout: GridLayoutManager
        when {
            prefs!!.numOfDice % 3 == 0 -> gLayout = GridLayoutManager(GlobalVariable.context, 3)
            prefs!!.numOfDice % 2 == 0 -> gLayout = GridLayoutManager(GlobalVariable.context, 2)
            else -> {
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
        }
        mLayoutManager = gLayout
        mRecyclerView?.layoutManager = mLayoutManager

        val adapter = mRecyclerView?.adapter as DiceRecyclerViewAdapter
        adapter.editMode = prefs!!.editMode
        if (clear) {
            adapter.clearDie(die)
        } else {
            adapter.setDie(die)
        }
        val callback = ItemMoveCallback(adapter)
        if (prefs!!.numOfDice > 3) {
            callback.setMoreThanOneRow()
        }
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper?.attachToRecyclerView(mRecyclerView)
        mRecyclerView?.adapter = adapter

        prefs!!.saveDice(die)
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

    private fun showIndeterminate() {
        if (checkShowAnswers.isChecked) {
            indeterminateBar.visibility = View.VISIBLE
        }
    }

    private fun clearAnswersView() {
        differenceText.visibility = View.INVISIBLE
        answerViewError.visibility = View.INVISIBLE
        showIndeterminate()
    }

    private fun setErrors(message: String) {
        indeterminateBar.visibility = View.GONE
        answerViewError.text = "Sorry, the following error occurred: $message"
        answerViewError.visibility = View.VISIBLE
    }

    override fun onNoteListChanged(l: List<Die>) {

    }


    override fun onResume() {
        mRewardedVideoAd.resume(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        super.onResume()
    }

    override fun onPause() {
        mRewardedVideoAd.pause(this)
        super.onPause()
    }


    override fun onDestroy() {
        mRewardedVideoAd.destroy(this)
        job.cancel()
        super.onDestroy()
        //timer.cancel()
    }

    override fun onRewardedVideoAdLeftApplication() {
    }

    override fun onRewardedVideoAdLoaded() {
    }

    override fun onRewardedVideoAdOpened() {
    }

    override fun onRewardedVideoCompleted() {
    }

    override fun onRewarded(p0: RewardItem?) {
        var currentDateTime = Date()
        prefs!!.blockAdsEndTime = Date(currentDateTime.time + 2 * HOUR)
        rolls = 10
    }

    override fun onRewardedVideoStarted() {
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {
    }

    override fun onRewardedVideoAdClosed() {
        loadRewardedVideoAd()
    }
    private fun loadRewardedVideoAd() {
        if(isTest){
            mRewardedVideoAd.loadAd(videoTestAdId,
                AdRequest.Builder()
                    // .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)// this makes the video ads real for some reason
                    .build())
        }
        else {
            mRewardedVideoAd.loadAd(videoReleaseAdId,
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
            )
        }
    }
    private fun loadInterstitialAd() {
        if (isTest) {
            mInterstitialAd.loadAd(AdRequest.Builder()
                // .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build())
        } else {
            mInterstitialAd.loadAd(AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build())
        }
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