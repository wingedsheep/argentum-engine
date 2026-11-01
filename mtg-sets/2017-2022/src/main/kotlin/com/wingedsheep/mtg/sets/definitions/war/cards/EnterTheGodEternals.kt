package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enter the God-Eternals — War of the Spark #196 (canonical printing)
 * {2}{U}{U}{B}
 * Sorcery
 * Enter the God-Eternals deals 4 damage to target creature and you gain life equal to the damage
 * dealt this way. Target player mills four cards. Amass Zombies 4.
 *
 * The gain reads the damage actually dealt (`damageDealtVariable`): an illegal creature target or
 * prevented damage gains nothing, while the mill and the amass still happen as long as one target
 * is legal.
 */
val EnterTheGodEternals = card("Enter the God-Eternals") {
    manaCost = "{2}{U}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Enter the God-Eternals deals 4 damage to target creature and you gain life equal to the " +
        "damage dealt this way. Target player mills four cards. Amass Zombies 4. (Put four +1/+1 counters " +
        "on an Army you control. It's also a Zombie. If you don't control an Army, create a 0/0 black " +
        "Zombie Army creature token first.)"

    spell {
        val creature = target(TargetFilter.Creature)
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val dealt = runStoringNumber { Effects.DealDamage(4, creature, damageDealtVariable = it) }
            run(Effects.GainLife(dealt.amount))
        } then Patterns.Library.mill(4, player) then Effects.Amass(4, "Zombie")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f67e7070-2266-4c93-8c89-084e12ba7a4e.jpg?1783933397"
        ruling("2019-05-03", "You can't cast Enter the God-Eternals without targeting a creature and a player.")
        ruling("2019-05-03", "If the target creature has less than 4 toughness, Enter the God-Eternals still deals 4 damage to it and you'll gain 4 life.")
        ruling("2019-05-03", "If the target creature is no longer a legal target as the spell resolves but the player is a legal target, no creature is dealt damage and you gain no life, but the target player still moves the top cards of their library and you still amass 4.")
        ruling("2019-05-03", "If the target player is no longer a legal target as the spell resolves but the creature is a legal target, the creature is dealt damage, you gain that much life, and you amass 4. No player moves cards from their library.")
        ruling("2019-05-03", "If both targets are no longer legal targets as the spell tries to resolve, it doesn't resolve. You don't amass 4.")
        ruling("2019-05-03", "If the target creature is dealt lethal damage by Enter the God-Eternals, it'll still be on the battlefield while you gain life, the target player moves cards, and you amass 4.")
    }
}
