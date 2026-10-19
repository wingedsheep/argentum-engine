package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tatsunari, Toad Rider
 * {2}{B}
 * Legendary Creature — Human Ninja
 * 3/3
 * Whenever you cast an enchantment spell, if you don't control a creature named Keimi, create Keimi,
 * a legendary 3/3 black and green Frog creature token with "Whenever you cast an enchantment spell,
 * each opponent loses 1 life and you gain 1 life."
 * {1}{G/U}: Tatsunari and target Frog you control can't be blocked this turn except by creatures
 * with flying or reach.
 */
private val flyingOrReach = GameObjectFilter.Creature.withCardPredicate(
    CardPredicate.Or(listOf(CardPredicate.HasKeyword(Keyword.FLYING), CardPredicate.HasKeyword(Keyword.REACH)))
)

val TatsunariToadRider = card("Tatsunari, Toad Rider") {
    manaCost = "{2}{B}"
    colorIdentity = "UBG"
    typeLine = "Legendary Creature — Human Ninja"
    power = 3
    toughness = 3
    oracleText = "Whenever you cast an enchantment spell, if you don't control a creature named Keimi, " +
        "create Keimi, a legendary 3/3 black and green Frog creature token with \"Whenever you cast an " +
        "enchantment spell, each opponent loses 1 life and you gain 1 life.\"\n" +
        "{1}{G/U}: Tatsunari and target Frog you control can't be blocked this turn except by creatures " +
        "with flying or reach."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Enchantment)
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Creature.named("Keimi"),
            negate = true
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Frog"),
            name = "Keimi",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/b/fbb5b3f9-0012-4deb-b795-c377cdfe546b.jpg?1783923712",
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.you.casts(GameObjectFilter.Enchantment),
                    effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
                        Effects.GainLife(1)
                )
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G/U}")
        val frog = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Frog").youControl()))
        effect = Effects.GrantCantBeBlockedExceptBy(EffectTarget.Self, flyingOrReach) then
            Effects.GrantCantBeBlockedExceptBy(frog, flyingOrReach)
        description = "Tatsunari and target Frog you control can't be blocked this turn except by " +
            "creatures with flying or reach."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abf42833-43d0-4b05-b499-d13b2c577ee8.jpg?1783923876"
    }
}
