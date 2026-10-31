package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lochmere Serpent — Throne of Eldraine #195
 * {4}{U}{B} · Creature — Serpent · 7/7
 *
 * Flash
 * {U}, Sacrifice an Island: This creature can't be blocked this turn.
 * {B}, Sacrifice a Swamp: You gain 1 life and draw a card.
 * {U}{B}: Exile five target cards from an opponent's graveyard. Return this card from your
 * graveyard to your hand. Activate only as a sorcery.
 *
 * "An opponent's graveyard" is a single graveyard: the five targets are opponent-owned graveyard
 * cards bound together by the cross-target `sameOwner` constraint (Rag Dealer's shape). The count is
 * exactly five, not "up to", so the ability can't be activated with fewer available (2019-10-04
 * ruling). If only some targets are illegal on resolution, the rest are exiled and the Serpent still
 * returns; if all are illegal the whole ability doesn't resolve.
 */
val LochmereSerpent = card("Lochmere Serpent") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Serpent"
    power = 7
    toughness = 7
    oracleText = "Flash\n" +
        "{U}, Sacrifice an Island: This creature can't be blocked this turn.\n" +
        "{B}, Sacrifice a Swamp: You gain 1 life and draw a card.\n" +
        "{U}{B}: Exile five target cards from an opponent's graveyard. Return this card from your " +
        "graveyard to your hand. Activate only as a sorcery."

    keywords(Keyword.FLASH)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{U}"),
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.ISLAND)),
        )
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "Can't be blocked this turn"
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype(Subtype.SWAMP)),
        )
        effect = Effects.GainLife(1) then Effects.DrawCards(1)
        description = "Gain 1 life and draw a card"
    }

    activatedAbility {
        cost = Costs.Mana("{U}{B}")
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        targets(TargetFilter.CardInGraveyard.ownedByOpponent(), count = 5, sameOwner = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE)
        ) then Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        description = "Exile five target cards from an opponent's graveyard and return this card to your hand"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Sam Burley"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/3287beea-747c-4cb6-aea5-051e85c5de8d.jpg?1783932595"
        ruling(
            "2019-10-04",
            "You can't activate Lochmere Serpent's last ability without targeting five cards in a " +
                "single opponent's graveyard.",
        )
        ruling(
            "2019-10-04",
            "If up to four of the target cards are illegal targets by the time Lochmere Serpent's last " +
                "ability resolves, the remaining legal targets are exiled and Lochmere Serpent is returned " +
                "to your hand.",
        )
        ruling(
            "2019-10-04",
            "Activating Lochmere Serpent's first activated ability after it has become blocked won't " +
                "cause it to become unblocked.",
        )
    }
}
