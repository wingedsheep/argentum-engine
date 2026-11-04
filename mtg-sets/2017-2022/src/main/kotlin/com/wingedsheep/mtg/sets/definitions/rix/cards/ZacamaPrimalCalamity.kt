package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Zacama, Primal Calamity — Rivals of Ixalan #174
 * {6}{R}{G}{W} · Legendary Creature — Elder Dinosaur · 9/9 · Mythic
 *
 * Reach, vigilance, trample
 * When Zacama enters, if you cast it, untap all lands you control.
 * {2}{R}: Zacama deals 3 damage to target creature.
 * {2}{G}: Destroy target artifact or enchantment.
 * {2}{W}: You gain 3 life.
 *
 * "If you cast it" is the intervening-if [Conditions.WasCast] (cast from any zone, CR 603.4), so a
 * reanimated or blinked Zacama untaps nothing. The untap is the Nature's Will group untap over
 * lands you control — including the ones just tapped to cast it, which then pay for the abilities.
 */
val ZacamaPrimalCalamity = card("Zacama, Primal Calamity") {
    manaCost = "{6}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Elder Dinosaur"
    power = 9
    toughness = 9
    oracleText = "Reach, vigilance, trample\n" +
        "When Zacama enters, if you cast it, untap all lands you control.\n" +
        "{2}{R}: Zacama deals 3 damage to target creature.\n" +
        "{2}{G}: Destroy target artifact or enchantment.\n" +
        "{2}{W}: You gain 3 life."

    keywords(Keyword.REACH, Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        effect = Patterns.Group.untapGroup(GroupFilter(GameObjectFilter.Land.youControl()))
        description = "When Zacama enters, if you cast it, untap all lands you control."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(3, t)
        description = "{2}{R}: Zacama deals 3 damage to target creature."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
        description = "{2}{G}: Destroy target artifact or enchantment."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.GainLife(3)
        description = "{2}{W}: You gain 3 life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5aa75f2b-53c5-47c5-96d2-ab796358a96f.jpg?1783935269"
        ruling(
            "2018-01-19",
            "Zacama's triggered ability triggers if you cast it from any zone. It doesn't trigger if " +
                "you put Zacama onto the battlefield without casting it."
        )
        ruling(
            "2023-07-28",
            "Zacama's damage-dealing ability can be activated during combat, perhaps targeting a " +
                "creature blocking it. If all creatures blocking Zacama are destroyed, its combat damage " +
                "is assigned to the player, planeswalker, or battle it's attacking because of trample. If " +
                "the blocking creatures are dealt nonlethal damage, that damage is considered when " +
                "assigning trample damage."
        )
    }
}
