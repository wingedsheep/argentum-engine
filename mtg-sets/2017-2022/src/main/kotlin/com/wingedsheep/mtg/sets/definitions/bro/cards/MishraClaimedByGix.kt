package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mishra, Claimed by Gix
 * {2}{B}{R}
 * Legendary Creature — Phyrexian Human Artificer
 * 3/5
 * Whenever you attack, each opponent loses X life and you gain X life, where X is the number of
 * attacking creatures. If Mishra, Claimed by Gix and a creature named Phyrexian Dragon Engine are
 * attacking, and you both own and control them, exile them, then meld them into Mishra, Lost to
 * Phyrexia. It enters tapped and attacking.
 *
 * X counts every attacking creature at resolution. "You gain X life" is not "life lost this way"
 * ([Effects.DrainLife]): you gain X once, however many opponents there are and whether or not they
 * could lose life. The meld half is checked as the trigger resolves (not an intervening if): Mishra
 * must still be attacking, and [Effects.Meld] only picks an *attacking* Phyrexian Dragon Engine you
 * own and control, re-checking ownership and control itself. The melded permanent enters tapped and
 * attacking but was never declared as an attacker, so its own attack trigger doesn't fire — its
 * enters trigger does.
 */
val MishraClaimedByGix = card("Mishra, Claimed by Gix") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Phyrexian Human Artificer"
    power = 3
    toughness = 5
    oracleText = "Whenever you attack, each opponent loses X life and you gain X life, where X is the " +
        "number of attacking creatures. If Mishra, Claimed by Gix and a creature named Phyrexian Dragon " +
        "Engine are attacking, and you both own and control them, exile them, then meld them into " +
        "Mishra, Lost to Phyrexia. It enters tapped and attacking."

    triggeredAbility {
        trigger = Triggers.you.attacks()
        val attackers = DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count()
        val dragonEngine = GameObjectFilter.Creature.named(DRAGON_ENGINE).attacking()
        effect = Effects.LoseLife(attackers, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(attackers) then
            Effects.If(
                Conditions.All(
                    Conditions.SourceMatches(GameObjectFilter.Creature.attacking().ownedByYou().youControl()),
                    Conditions.YouControl(dragonEngine.ownedByYou())
                ),
                Effects.Meld(dragonEngine, into = "Mishra, Lost to Phyrexia", tappedAndAttacking = true)
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "216"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a469d7ec-42de-45de-9d8f-469bb979de58.jpg?1783920028"
    }
}

private const val DRAGON_ENGINE = "Phyrexian Dragon Engine"
