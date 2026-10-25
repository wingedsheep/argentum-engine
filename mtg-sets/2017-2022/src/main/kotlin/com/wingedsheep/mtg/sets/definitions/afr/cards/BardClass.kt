package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Bard Class {R}{G}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Legendary creatures you control enter with an additional +1/+1 counter on them.
 *
 * {R}{G}: Level 2
 * Legendary spells you cast cost {R}{G} less to cast. This effect reduces only the amount of
 * colored mana you pay.
 *
 * {3}{R}{G}: Level 3
 * Whenever you cast a legendary spell, exile the top two cards of your library. You may play them
 * this turn.
 *
 * Level 1 is Metallic Mimic's runtime [EntersWithCounters] replacement narrowed to legendary
 * creatures; level 2 is Edgewalker's colored-only [ModifySpellCost] (the cost scan reads the Class's
 * reached level, as for the Talents); level 3 is The Circle of Loyalty's legendary-cast trigger
 * feeding `Patterns.Exile.impulse`.
 */
val BardClass = card("Bard Class") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "Legendary creatures you control enter with an additional +1/+1 counter on them.\n" +
        "{R}{G}: Level 2 — Legendary spells you cast cost {R}{G} less to cast. This effect reduces " +
        "only the amount of colored mana you pay.\n" +
        "{3}{R}{G}: Level 3 — Whenever you cast a legendary spell, exile the top two cards of your " +
        "library. You may play them this turn."

    // Level 1: legendary creatures you control enter with an additional +1/+1 counter.
    replacementEffect(
        EntersWithCounters(
            count = 1,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.legendary().youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    // Level 2: legendary spells you cast cost {R}{G} less (colored mana only).
    classLevel(2, "{R}{G}") {
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.legendary()),
                modification = CostModification.ReduceColored("{R}{G}"),
            )
        }
    }

    // Level 3: whenever you cast a legendary spell, impulse the top two cards.
    classLevel(3, "{3}{R}{G}") {
        triggeredAbility {
            trigger = Triggers.you.casts(GameObjectFilter.Any.legendary())
            effect = Patterns.Exile.impulse(count = 2)
            description = "Whenever you cast a legendary spell, exile the top two cards of your library. " +
                "You may play them this turn."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37d6343a-c514-4ca6-a415-62d1a473ae20.jpg?1783926450"
        ruling("2021-07-23", "If you control multiple Bard Classes, legendary creatures you control enter the battlefield with that many additional +1/+1 counters.")
        ruling("2021-07-23", "Unlike most abilities that reduce the costs of spells, Bard Class's Level 2 ability cannot be applied to generic mana costs. For example, a spell that costs {2}{G} to cast would instead cost {2} to cast.")
        ruling("2021-07-23", "If you do not play the cards exiled with Bard Classes Level 3 ability on the turn they are exiled, they will remain exiled indefinitely and cannot be played on a later turn.")
        ruling("2021-07-23", "If you exile a modal double-faced card this way, you may cast either face of that card.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
