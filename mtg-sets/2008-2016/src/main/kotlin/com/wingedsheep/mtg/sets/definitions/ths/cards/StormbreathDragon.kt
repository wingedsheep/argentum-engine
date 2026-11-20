package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormbreath Dragon — Theros #143
 * {3}{R}{R} · Creature — Dragon · 4/4
 *
 * Flying, haste, protection from white
 * {5}{R}{R}: Monstrosity 3.
 * When this creature becomes monstrous, it deals damage to each opponent equal to the number of
 * cards in that player's hand.
 *
 * The per-opponent damage is a [Effects.ForEachPlayer] over [Player.EachOpponent], which rebinds
 * [Player.You] / [EffectTarget.Controller] to each opponent in turn, so the amount reads that
 * opponent's own hand (Fraying Omnipotence template). The damage source stays the Dragon
 * ([EffectTarget.Self]), unaffected by the rebind (Zoyowa Lava-Tongue template).
 */
val StormbreathDragon = card("Stormbreath Dragon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, haste, protection from white\n" +
        "{5}{R}{R}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)\n" +
        "When this creature becomes monstrous, it deals damage to each opponent equal to the number of cards in that player's hand."

    keywords(Keyword.FLYING, Keyword.HASTE)
    keywordAbility(KeywordAbility.protectionFrom(Color.WHITE))

    activatedAbility {
        cost = Costs.Mana("{5}{R}{R}")
        effect = Effects.Monstrosity(3)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesMonstrous()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.DealDamage(
                amount = DynamicAmounts.zone(Player.You, Zone.HAND).count(),
                target = EffectTarget.Controller,
                damageSource = EffectTarget.Self,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "143"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/471979de-f569-444a-8651-12d7e726949a.jpg?1783939753"
        ruling("2013-09-15", "For each opponent, count the number of cards in that player's hand when the last ability resolves to determine how much damage Stormbreath Dragon does to them.")
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
