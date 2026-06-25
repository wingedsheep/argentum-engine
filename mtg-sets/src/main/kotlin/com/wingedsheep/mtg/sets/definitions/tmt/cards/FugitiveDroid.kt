package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fugitive Droid
 * {U}
 * Artifact Creature — Robot Scientist
 * 1/1
 *
 * This creature can't be blocked if an artifact entered the battlefield under
 * your control this turn.
 * {U}, Sacrifice this creature: Counter target spell that targets an artifact or
 * creature you control.
 */
val FugitiveDroid = card("Fugitive Droid") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Scientist"
    oracleText = "This creature can't be blocked if an artifact entered the battlefield under your control this turn.\n{U}, Sacrifice this creature: Counter target spell that targets an artifact or creature you control."
    power = 1
    toughness = 1

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.PermanentTypeEnteredBattlefieldThisTurn(CardType.ARTIFACT, Player.You)
        )
    }

    activatedAbility {
        // "target spell that targets an artifact or creature you control" — a stack spell at
        // least one of whose chosen targets matches the subfilter (CardPredicate.TargetsMatching).
        val spell = target(
            "target spell that targets an artifact or creature you control",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.TargetsMatching(GameObjectFilter.CreatureOrArtifact.youControl())
                        )
                    ),
                    zone = Zone.STACK
                )
            )
        )
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Narendra Bintara Adi"
        flavorText = "\"A freak accident transferred Professor Honeycutt's mind into his robot assistant. Blamed for his own death, he fled . . . as the FUGITOID!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50c4e65f-00dc-4fc5-bd5c-8482c2848f4c.jpg?1771586801"
    }
}
