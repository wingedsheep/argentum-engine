package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Rangers of Ithilien
 * {2}{U}{U}
 * Creature — Human Ranger
 * 3/3
 *
 * Vigilance
 * When this creature enters, gain control of up to one target creature with lesser power for as long
 * as you control this creature. Then the Ring tempts you.
 *
 * "Lesser power" uses the new strict `CardPredicate.PowerLessThanEntity` / `powerLessThanEntity()`
 * (compared to the source's power); the control change uses `Duration.WhileYouControlSource`.
 */
val RangersOfIthilien = card("Rangers of Ithilien") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ranger"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "When this creature enters, gain control of up to one target creature with lesser power for " +
        "as long as you control this creature. Then the Ring tempts you."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "up to one target creature with lesser power",
            TargetCreature(
                optional = true,
                filter = TargetFilter(GameObjectFilter.Creature.powerLessThanEntity(EntityReference.Source))
            )
        )
        effect = Effects.GainControl(creature, Duration.WhileYouControlSource("Rangers of Ithilien"))
            .then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Torgeir Fjereide"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/819b4208-7a29-41fa-947f-614bf669b300.jpg?1686968256"
    }
}
