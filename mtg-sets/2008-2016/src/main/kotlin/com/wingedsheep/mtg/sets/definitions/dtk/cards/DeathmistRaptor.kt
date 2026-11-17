package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathmist Raptor
 * {1}{G}{G}
 * Creature — Dinosaur Beast
 * 3/3
 * Deathtouch
 * Whenever a permanent you control is turned face up, you may return this card from your
 * graveyard to the battlefield face up or face down.
 * Megamorph {4}{G}
 *
 * The return trigger functions only from the graveyard (`triggerZone = GRAVEYARD`), so it fires
 * only while the Raptor is there when the permanent turns face up (first ruling). Face up, face
 * down, or staying in the graveyard is decided as the ability resolves (second ruling), so it is
 * one `ChooseAction` with a "leave it" option for the "you may". The face-down return uses
 * `FaceDownMode.MORPH`: a 2/2 face-down creature whose turn-up procedure is the card's own
 * megamorph cost, so flipping it later also adds the +1/+1 counter. Both returns are gated on
 * `fromZone = GRAVEYARD`, so a Raptor that left the graveyard in response stays where it is.
 */
val DeathmistRaptor = card("Deathmist Raptor") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur Beast"
    power = 3
    toughness = 3
    oracleText = "Deathtouch\n" +
        "Whenever a permanent you control is turned face up, you may return this card from your " +
        "graveyard to the battlefield face up or face down.\n" +
        "Megamorph {4}{G} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.you.permanentTurnedFaceUp()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Return Deathmist Raptor face up",
                effect = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
            ),
            EffectChoice(
                label = "Return Deathmist Raptor face down",
                effect = Effects.Move(
                    EffectTarget.Self,
                    Zone.BATTLEFIELD,
                    fromZone = Zone.GRAVEYARD,
                    faceDown = FaceDownMode.MORPH,
                ),
            ),
            EffectChoice(label = "Leave it in your graveyard", effect = Effects.Nothing),
        ))
        description = "Whenever a permanent you control is turned face up, you may return Deathmist " +
            "Raptor from your graveyard to the battlefield face up or face down."
    }

    // Megamorph {4}{G}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{4}{G}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "180"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74c40df1-3f63-49e7-a869-1ce14f94a753.jpg?1783938581"
        ruling(
            "2015-02-25",
            "Deathmist Raptor's second ability triggers only if Deathmist Raptor is in the graveyard " +
                "when a permanent you control is turned face up.",
        )
        ruling(
            "2015-02-25",
            "You choose whether Deathmist Raptor will enter the battlefield face up or face down as " +
                "the ability resolves. You may also choose to leave it in the graveyard.",
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
