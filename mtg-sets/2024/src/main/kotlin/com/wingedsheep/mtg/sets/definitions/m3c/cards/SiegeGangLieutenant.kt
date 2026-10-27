package com.wingedsheep.mtg.sets.definitions.m3c.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Siege-Gang Lieutenant — Modern Horizons 3 Commander #113
 * {3}{R} · Creature — Goblin · 2/2
 * Lieutenant — At the beginning of combat on your turn, if you control your commander, create two
 * 1/1 red Goblin creature tokens. Those tokens gain haste until end of turn.
 * {2}, Sacrifice a Goblin: This creature deals 1 damage to any target.
 *
 * The lieutenant trigger is [com.wingedsheep.mtg.sets.definitions.c18.cards.LoyalApprentice]'s
 * shape: an intervening-if (CR 603.4) over a battlefield commander that you also *own* — "your
 * commander", so a stolen opponent's commander doesn't count. "Those tokens gain haste" iterates
 * the [CollectionSlot.CreatedTokens] collection the create-token step publishes, granting haste for
 * the turn only (Sokenzan, Crucible of Defiance) rather than printing it on the tokens.
 *
 * "Sacrifice a Goblin" is any Goblin permanent, so the cost filter is a permanent with the Goblin
 * subtype, not only creatures (Goblin Chirurgeon, Arms Dealer).
 */
val SiegeGangLieutenant = card("Siege-Gang Lieutenant") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2
    oracleText = "Lieutenant — At the beginning of combat on your turn, if you control your commander, " +
        "create two 1/1 red Goblin creature tokens. Those tokens gain haste until end of turn.\n" +
        "{2}, Sacrifice a Goblin: This creature deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouControl(GameObjectFilter.Permanent.commander().ownedByYou())
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/d/a/daab2f6a-ea1d-49ec-9457-4c5db6cc6538.jpg?1783911125",
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity),
        )
        description = "Lieutenant — At the beginning of combat on your turn, if you control your " +
            "commander, create two 1/1 red Goblin creature tokens. Those tokens gain haste until end of turn."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Goblin")),
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Warren Mahy"
        flavorText = "His crew proudly maintains an accident rate of 100% per day."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e22c2df5-ae00-4d71-ac60-6ea402b9036b.jpg?1783911410"
        ruling("2024-06-07", "If you don't control your commander as the lieutenant ability resolves, you won't get its effect.")
        ruling("2024-06-07", "If you have multiple commanders, the lieutenant effect will happen as long as you control at least one commander. It will happen only once even if you control multiple commanders.")
    }
}
