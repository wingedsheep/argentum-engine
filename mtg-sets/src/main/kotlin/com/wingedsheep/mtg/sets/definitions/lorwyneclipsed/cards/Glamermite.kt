package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val Glamermite = card("Glamermite") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 2
    oracleText = "Flash\nFlying\nWhen this creature enters, choose one —\n• Tap target creature.\n• Untap target creature."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.Tap(EffectTarget.ContextTarget(0)),
                Targets.Creature,
                "Tap target creature"
            ),
            Mode.withTarget(
                Effects.Untap(EffectTarget.ContextTarget(0)),
                Targets.Creature,
                "Untap target creature"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Pauline Voss"
        flavorText = "\"And watch, my queen, he'll do as I please!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8b7c23a-0034-453c-ab44-f6ec0f31d1eb.jpg?1767956988"
    }
}
