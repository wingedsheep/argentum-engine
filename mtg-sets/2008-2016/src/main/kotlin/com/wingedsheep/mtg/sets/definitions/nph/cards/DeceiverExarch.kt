package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deceiver Exarch
 * {2}{U}
 * Creature — Phyrexian Cleric
 * 1/4
 * Flash (You may cast this spell any time you could cast an instant.)
 * When this creature enters, choose one —
 * • Untap target permanent you control.
 * • Tap target permanent an opponent controls.
 *
 * Flash is a printed [Keyword]. The enters trigger is a [ModalEffect.chooseOne] on
 * [Triggers.EntersBattlefield], each mode carrying its own target — the mode is what decides
 * *whose* permanent is legal, so both are [Mode.withTarget] over [Targets.PermanentYouControl] and
 * [Targets.PermanentOpponentControls] rather than one shared requirement. The effects are the two
 * halves of the same tap/untap atom, [Effects.Untap] and [Effects.Tap], each reading its own
 * mode's slot.
 */
val DeceiverExarch = card("Deceiver Exarch") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Cleric"
    power = 1
    toughness = 4
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, choose one —\n" +
        "• Untap target permanent you control.\n" +
        "• Tap target permanent an opponent controls."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                effect = Effects.Untap(EffectTarget.ContextTarget(0)),
                target = Targets.PermanentYouControl,
                description = "Untap target permanent you control."
            ),
            Mode.withTarget(
                effect = Effects.Tap(EffectTarget.ContextTarget(0)),
                target = Targets.PermanentOpponentControls,
                description = "Tap target permanent an opponent controls."
            )
        )
        description = "When this creature enters, choose one — Untap target permanent you control; " +
            "or tap target permanent an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f123ad6-fe84-4fed-9c0f-6b41921e9c26.jpg"
    }
}
