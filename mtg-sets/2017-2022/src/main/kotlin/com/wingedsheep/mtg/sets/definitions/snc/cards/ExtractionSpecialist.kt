package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Extraction Specialist
 * {2}{W}
 * Creature — Human Rogue
 * 3/2
 *
 * Lifelink
 * When this creature enters, return target creature card with mana value 2 or less from your
 * graveyard to the battlefield. That creature can't attack or block for as long as you control
 * this creature.
 *
 * - The reanimation is tracked (`moveTracked`) so the restriction lands only on the creature that
 *   actually arrived on the battlefield — Kheru Lich Lord's shape.
 * - "For as long as you control this creature" is `Duration.WhileYouControlSource`, the same duration
 *   There and Back Again's "can't block for as long as you control" uses. Per the ruling, if the
 *   Specialist has already left the battlefield when the trigger resolves, the gate is closed from the
 *   start and the returned creature attacks and blocks as normal.
 */
val ExtractionSpecialist = card("Extraction Specialist") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 2
    oracleText = "Lifelink\n" +
        "When this creature enters, return target creature card with mana value 2 or less from your " +
        "graveyard to the battlefield. That creature can't attack or block for as long as you control " +
        "this creature."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(2), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val returned = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD)).asTarget
            run(Effects.CantAttackOrBlock(returned, Duration.WhileYouControlSource("Extraction Specialist")))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b404d6c7-0b65-4c6a-b141-9dffbeb120db.jpg?1783923159"
        ruling("2022-04-29", "Once Extraction Specialist leaves the battlefield, the creature that is returned can attack and block as normal. If Extraction Specialist leaves the battlefield before its ability resolves, the creature that is returned will be able to attack and block as normal right away.")
    }
}
