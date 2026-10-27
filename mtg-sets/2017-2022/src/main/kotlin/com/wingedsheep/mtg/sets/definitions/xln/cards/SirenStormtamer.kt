package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Siren Stormtamer
 * {U}
 * Creature — Siren Pirate Wizard
 * 1/1
 * Flying
 * {U}, Sacrifice this creature: Counter target spell or ability that targets you or a creature you
 * control.
 *
 * "Targets you or a creature you control" is the disjunction of the player half
 * ([CardPredicate.TargetsPlayer]) and the object half ([CardPredicate.TargetsMatching]) of the
 * stack-targeting predicates. It sits in both branches of the spell-or-ability union so the
 * ability branch is named explicitly (abilities are only offered as stack targets when the filter
 * names one). Both predicates read the stack object's chosen targets at resolution too, so the
 * targeted creature leaving the battlefield makes Stormtamer's target illegal, while the creature
 * merely becoming an illegal target for that spell does not (2020-11-10 rulings).
 */
val SirenStormtamer = card("Siren Stormtamer") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren Pirate Wizard"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "{U}, Sacrifice this creature: Counter target spell or ability that targets you or a creature you control."

    keywords(Keyword.FLYING)

    activatedAbility {
        val targetsYouOrYourCreature = CardPredicate.Or(
            listOf(
                CardPredicate.TargetsPlayer(Player.You),
                CardPredicate.TargetsMatching(GameObjectFilter.Creature.youControl()),
            )
        )
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        target(
            TargetFilter(
                GameObjectFilter(
                    anyOf = listOf(
                        GameObjectFilter(cardPredicates = listOf(targetsYouOrYourCreature)),
                        GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.IsActivatedOrTriggeredAbility,
                                targetsYouOrYourCreature,
                            )
                        ),
                    )
                ),
                zone = Zone.STACK,
            )
        )
        effect = Effects.CounterSpellOrAbility()
        description = "{U}, Sacrifice this creature: Counter target spell or ability that targets you " +
            "or a creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Josu Hernaiz"
        flavorText = "He's the calm that hides the coming storm."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72f7375c-2d9b-4738-b539-00e1949980e7.jpg?1783935772"
        ruling(
            "2020-11-10",
            "Siren Stormtamer's activated ability can target a spell or ability that has multiple targets, " +
                "as long as at least one of those targets is you or a creature you control."
        )
        ruling(
            "2020-11-10",
            "If the creature you control targeted by the target spell or ability leaves the battlefield, " +
                "that spell or ability is no longer a legal target for Siren Stormtamer's ability. On the " +
                "other hand, if that targeted creature becomes an illegal target for the target spell but " +
                "remains on the battlefield under your control, the target spell or ability is still a " +
                "legal target for Siren Stormtamer's ability."
        )
    }
}
