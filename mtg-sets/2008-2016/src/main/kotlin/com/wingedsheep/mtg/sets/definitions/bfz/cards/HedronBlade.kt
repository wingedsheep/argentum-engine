package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hedron Blade
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Whenever equipped creature becomes blocked by one or more colorless creatures, it gains
 * deathtouch until end of turn.
 * Equip {2}
 *
 * "One or more" is a single trigger however many colorless creatures block
 * (`becomesBlocked(oncePerCombat = true)`); "it" is the equipped creature, read with last-known
 * attachment if the Blade leaves before the trigger resolves.
 */
val HedronBlade = card("Hedron Blade") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature becomes blocked by one or more colorless creatures, it gains " +
        "deathtouch until end of turn. (Any amount of damage it deals to a creature is enough to destroy it.)\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.becomesBlocked(
            by = GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
            oncePerCombat = true
        )
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87242e1d-babe-4b0d-aa1f-ab62e0bc4ab7.jpg?1783938177"
    }
}
