package com.storm.pepper;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.ListenBuilder;
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.PhraseSet;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.util.FutureUtils;
import com.storm.posh.BaseBehaviourLibrary;
import com.storm.posh.plan.planelements.Sense;
import com.storm.posh.plan.planelements.action.ActionEvent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class HonestyBehaviourLibraryLA extends BaseBehaviourLibrary {
    private static final String TAG = HonestyBehaviourLibraryLA.class.getSimpleName();

    //booleans used for the senses
    private boolean humanReady;
    private boolean rules;
    private boolean transition;


    @Override
    public void reset() {
        super.reset();

        humanReady = false;
        rules = false;
        transition = false;
    }

    @Override
    public boolean getBooleanSense(Sense sense) {
        //        pepperLog.appendLog(TAG, String.format("Getting boolean sense: %s", sense));
        boolean senseValue;

        switch (sense.getNameOfElement()) {
            //same senses as more anthro version
            case "HumanReady":
                senseValue = humanReady;
                break;
            case "Rules":
                senseValue = rules;
                break;
            case "End":
                senseValue = transition;
                break;

            default:
                senseValue = super.getBooleanSense(sense);
                break;
        }

        pepperLog.checkedBooleanSense(TAG, sense, senseValue);

        return senseValue;
    }

    @Override
    public void executeAction(ActionEvent action) {
        pepperLog.appendLog(TAG, "Performing action: " + action);

        if (action.getNameOfElement() == currentAction) {
            // already performing this action
            pepperLog.appendLog(TAG, String.format("Action still in progress: %s", currentAction));
            return;

        }

        switch (action.getNameOfElement()) {
            //same method call structure as other version
            case "Introduction":
                rules();
                break;
            case "Honesty":
                honestyGame();
                break;
            case "Transition":
                transition();
                break;

            default:
                super.executeAction(action);
                break;
        }
    }

    public void rules() {
        pepperLog.appendLog(TAG, "starting introduction");
        //https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/abilities/tuto/autonomous_abilities_tutorial.html
        //modified it to extend from base functionality of basic awareness to include blinking and movement
        holdAwareness();//baseBehaviour function to suspend the autonomous functions

        if (talking) {
            pepperLog.appendLog(TAG,"Cannot askForResult as already talking");
            return;
        } else if (listening) {
            pepperLog.appendLog(TAG, "Cannot askForResult as already listening");
            return;
        }

        setActive();
        this.talking = true;
        this.listening = true;

        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }

        if (!humanPresent) {
            pepperLog.appendLog(TAG, "no human present");
            return;
        } else {
            FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
                Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                        //rspd=70
                        .withText("\\vct=70\\ \\readmode=word\\The task is to roll the die that is on the table, and then log the result on " +
                                "the laptop. Participants are allowed to roll the die several times to check for bias, but only the first throw should be reported. The monetary payoff for a roll is shown on the chart provided " +
                                "i do not know the outcome of the participant’s roll till they inform me of their result. " +
                                "Is the Participant ready to start the game?") // Set the text to say.

                        .withBodyLanguageOption(BodyLanguageOption.DISABLED) // disable the motion of Pepper.
                        .build(); // Build the say action.

                say.run();

                // Create a topic.
                Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                        .withResource(R.raw.honesty_rules_la) // Set the topic resource.
                        .build(); // Build the topic.

                // Create a new QiChatbot.
                QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                        .withTopic(topic)
                        .build();
                qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED);

                QiChatVariable rulesBool = qiChatbot.variable("rules");

                rulesBool.addOnValueChangedListener(currentValue -> {
                    Log.i(TAG, "chatRules: " + String.valueOf(currentValue));
                    this.rules = Boolean.valueOf(currentValue);
                    //this.reset();
                });

                // Create a new Chat action.
                chat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();
                chat.setListeningBodyLanguage(BodyLanguageOption.DISABLED); // immobile while listening

                //listener
                chat.addOnStartedListener(() -> Log.d(TAG, "chat started"));

                // Run the Chat action asynchronously.
                Future<Void> chatFuture = chat.async().run();

                // Stop the chat when done
                qiChatbot.addOnEndedListener(endReason -> {
                    pepperLog.appendLog(TAG, String.format("Chat ended: %s", endReason));
                    chatFuture.requestCancellation();
                });

                // Add a lambda to the action execution.
                chatFuture.thenConsume(future -> {
                    pepperLog.appendLog(TAG, "Chat completed?");
                    this.talking = false;
                    this.listening = false;
                    pepperLog.appendLog(TAG, String.format("Rules: %s", this.rules));
                    if (future.hasError()) {
                        Log.d(TAG, "Discussion finished with error.", future.getError());
                    }
                });
            });
        }
    }

    public void honestyGame() {
        pepperLog.appendLog("Playing the honesty game");
        holdAwareness();//baseBehaviour function to suspend the autonomous functions
        if (talking) {
            pepperLog.appendLog(TAG,"Cannot askForResult as already talking");
            return;
        } else if (listening) {
            pepperLog.appendLog(TAG, "Cannot askForResult as already listening");
            return;
        }

        setActive();
        this.talking = true;
        this.listening = true;

        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }

        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText("\\vct=70\\ \\readword=word\\State when the die has been rolled and lack of bias confirmed") // Set the text to say.
                    .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                    .build(); // Build the say action.


            say.run();

            // Create a topic.
            Topic topic = TopicBuilder.with(qiContext) // Create the builder using the QiContext.
                    .withResource(R.raw.game_rules_la) // Set the topic resource.
                    .build(); // Build the topic.

            // Create a new QiChatbot.
            QiChatbot qiChatbot = QiChatbotBuilder.with(qiContext)
                    .withTopic(topic)
                    .build();
            qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED); //immobile while speaking

            QiChatVariable transitionBool = qiChatbot.variable("transition");

            transitionBool.addOnValueChangedListener(currentValue -> {
                Log.i(TAG, "chatTransition: " + String.valueOf(currentValue));
                this.transition = Boolean.valueOf(currentValue);
                //this.reset();
            });


            // Create a new Chat action.
            chat = ChatBuilder.with(qiContext)
                    .withChatbot(qiChatbot)
                    .build();
            chat.setListeningBodyLanguage(BodyLanguageOption.DISABLED); // immobile while listening

            // Add an on started listener to the Chat action.
            chat.addOnStartedListener(() -> Log.d(TAG, "Chat started."));

            // Run the Chat action asynchronously.
            Future<Void> chatFuture = chat.async().run();

            // Stop the chat when done
            qiChatbot.addOnEndedListener(endReason -> {
                pepperLog.appendLog(TAG, String.format("Chat ended: %s", endReason));
                chatFuture.requestCancellation();
            });

            // Add a lambda to the action execution.
            chatFuture.thenConsume(future -> {
                pepperLog.appendLog(TAG, "Chat completed?");
                this.talking = false;
                this.listening = false;
                pepperLog.appendLog(TAG, String.format("End: %s", this.transition));
                if (future.hasError()) {
                    Log.d(TAG, "Discussion finished with error.", future.getError());
                }
            });
        });
    }
    public void transition(){
        holdAwareness();//baseBehaviour function to suspend the autonomous functions
        if (lookAtFuture != null) {
            pepperLog.appendLog(TAG, "Stop looking");
            lookAtFuture.requestCancellation();
        }
        pepperLog.appendLog("Transitioning to next experiment");
        FutureUtils.wait(0, TimeUnit.SECONDS).andThenConsume((ignore) -> {
            Say say = SayBuilder.with(qiContext) // Create the builder with the context.
                    .withText("\\vct=70\\ \\readmode=word\\Please remain where you are while the next experiment is being" +
                            "prepared") // Set the text to say.
                    .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                    .build(); // Build the say action.

            say.run();

        });
        transition = false;
        humanReady = false;
        rules = false;
    }





}
