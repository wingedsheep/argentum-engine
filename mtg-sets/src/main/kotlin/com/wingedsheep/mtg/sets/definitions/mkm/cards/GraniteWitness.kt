package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

/**
 * Granite Witness — Murders at Karlov Manor #206
 * {2}{W}{U} · Artifact Creature — Gargoyle Detective · 3/2
 *
 * Flying, vigilance
 * Disguise {W/U}{W/U}
 * When this creature is turned face up, you may tap or untap target creature.
 *
 * The common WU disguise trick: hold up two hybrid mana and flip it mid-combat either to tap a
 * would-be blocker before blocks, or to untap one of yours after it attacked. Vigilance on the
 * face-up side makes the second line free.
 *
 * "You may tap or untap target creature" is the [SewerVeillanceCam] idiom — a [MayEffect] wrapping
 * a two-[Mode] [ModalEffect] over the single declared target, with `countsAsModalSpell = false` so
 * the tap/untap choice isn't mistaken for a modal *spell*. The target is chosen when the trigger
 * goes on the stack; the tap-or-untap choice is made on resolution, so an opponent who taps the
 * creature in response doesn't lock you into the now-useless half.
 *
 * The trigger is a real triggered ability, not a replacement — it uses the stack and can be
 * responded to, unlike the flip itself (a special action, CR 701.34a). It fires only on *this*
 * creature turning face up ([Triggers.TurnedFaceUp] is SELF-bound), and never on the card entering
 * face up: turning face up is not entering (CR 707.9a).
 */
val GraniteWitness = card("Granite Witness") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact Creature — Gargoyle Detective"
    oracleText = "Flying, vigilance\n" +
        "Disguise {W/U}{W/U} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, you may tap or untap target creature."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    disguise = "{W/U}{W/U}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val creature = target("target creature", Targets.Creature)
        effect = MayEffect(
            ModalEffect(
                modes = listOf(
                    Mode.noTarget(TapUntapEffect(creature, tap = true), "Tap that creature"),
                    Mode.noTarget(TapUntapEffect(creature, tap = false), "Untap that creature")
                ),
                chooseCount = 1,
                countsAsModalSpell = false
            )
        )
        description = "When this creature is turned face up, you may tap or untap target creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daee9d98-f8c6-4980-8f23-c6c636b69430.jpg?1783912849"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
        ruling(
            "2024-02-02",
            "Turning a permanent face up or face down doesn't change whether that permanent is " +
                "tapped or untapped."
        )
    }
}
