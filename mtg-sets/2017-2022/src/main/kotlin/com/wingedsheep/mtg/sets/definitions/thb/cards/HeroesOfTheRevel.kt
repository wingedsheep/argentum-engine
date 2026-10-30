package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Heroes of the Revel
 * {4}{R}
 * Creature — Satyr Soldier
 * 4/4
 * When this creature enters, create a 1/1 red Satyr creature token with "This token can't block."
 * Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn.
 *
 * Anax, Hardened in the Forge's Satyr token (a [CantBlock] static on the token) plus the Hero of
 * the Pride trigger.
 */
val HeroesOfTheRevel = card("Heroes of the Revel") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Satyr Soldier"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, create a 1/1 red Satyr creature token with \"This token can't block.\"\nWhenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Satyr"),
            staticAbilities = listOf(CantBlock()),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/903e30f3-580e-4a14-989b-ae0632363407.jpg?1783931453",
        )
        description = "When this creature enters, create a 1/1 red Satyr creature token with \"This token can't block.\""
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
        description = "Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Zoltan Boros"
        flavorText = "Brashness is close enough to bravery."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfdfb481-3446-42f4-a1c3-a88b69f2189a.jpg?1783931550"
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "If you cast a spell that targets this creature multiple times, this ability triggers just once.")
        ruling("2020-01-24", "This ability doesn't trigger if you copy a spell that targets it, or if a spell's targets are changed to target it.")
        ruling("2020-01-24", "This effect affects only creatures you control at the time the ability resolves. Creatures you begin to control later in the turn won't get +1/+0.")
    }
}
