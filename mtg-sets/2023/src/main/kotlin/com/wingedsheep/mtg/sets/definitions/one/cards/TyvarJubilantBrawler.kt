package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tyvar, Jubilant Brawler — Phyrexia: All Will Be One #218
 * {1}{B}{G} · Legendary Planeswalker — Tyvar · Starting loyalty 3
 *
 * You may activate abilities of creatures you control as though those creatures had haste.
 * +1: Untap up to one target creature.
 * −2: Mill three cards, then you may return a creature card with mana value 2 or less from your
 *     graveyard to the battlefield.
 *
 * The static is the same permission Thousand-Year Elixir carries — not a haste grant, so it never
 * lets a creature attack (2023-02-04 ruling). The −2's return is a resolution-time "up to one"
 * choice over the *whole* graveyard after the mill, not only the milled cards (2023-02-04 ruling).
 */
val TyvarJubilantBrawler = card("Tyvar, Jubilant Brawler") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Tyvar"
    startingLoyalty = 3
    oracleText = "You may activate abilities of creatures you control as though those creatures had haste.\n" +
        "+1: Untap up to one target creature.\n" +
        "−2: Mill three cards, then you may return a creature card with mana value 2 or less from " +
        "your graveyard to the battlefield."

    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.MAY_ACTIVATE_ABILITIES_AS_THOUGH_HASTY.name,
            GroupFilter.AllCreaturesYouControl,
        )
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.Untap(creature)
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            run(Patterns.Library.mill(3))
            val graveyard = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature.manaValueAtMost(2))
            )
            val chosen = chooseUpTo(
                1,
                from = graveyard,
                showAllCards = true,
                prompt = "You may return a creature card with mana value 2 or less to the battlefield",
                selectedLabel = "Return to the battlefield",
                remainderLabel = "Leave in graveyard"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "218"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66605fe1-9a20-4c95-b53e-1249cedb978b.jpg?1783917995"
        ruling("2023-02-04", "Tyvar's first ability doesn't grant haste to any creatures, nor does it allow you to attack with creatures as though they had haste. However, you will be able to activate abilities with {T} in their activation costs as soon as creatures with such abilities come under your control.")
        ruling("2023-02-04", "For Tyvar's last ability, you may return any creature card with mana value 2 or less from your graveyard to the battlefield, even if it wasn't one of the three cards milled as the ability resolved.")
    }
}
