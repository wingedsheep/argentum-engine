package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captain Marvel, Earth's Protector — Marvel Super Heroes #11 (mythic)
 * {3}{W}{W} · Legendary Creature — Human Kree Hero · 5/4
 *
 * Flash
 * Flying, lifelink
 * Power-up — {5}{W}{W}: Put a +1/+1 counter and an indestructible counter on Captain Marvel.
 * (Activate each power-up ability only once. Reduce the cost by her mana cost if she entered
 * this turn.)
 *
 * Flash and power-up are a deliberate pair, but only within a single turn: "entered this turn" is
 * about the turn, not about whose turn it is, so flashing her in during the opponent's turn lets
 * you power up for `{5}{W}{W}` − `{3}{W}{W}` = `{2}` *that same turn* — ambush blocker into a
 * two-mana indestructible 6/5. Waiting until you untap does not work: that is a new turn, she did
 * not enter during it, and the ability costs the printed `{5}{W}{W}`.
 *
 * The indestructible counter is [Counters.INDESTRUCTIBLE], a keyword counter (CR 122.1d) rather
 * than a granted static ability: it rides on the permanent, so it survives losing abilities and
 * is copied by anything that copies counters.
 */
val CaptainMarvelEarthsProtector = card("Captain Marvel, Earth's Protector") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Kree Hero"
    oracleText = "Flash\n" +
        "Flying, lifelink\n" +
        "Power-up — {5}{W}{W}: Put a +1/+1 counter and an indestructible counter on Captain " +
        "Marvel. (Activate each power-up ability only once. Reduce the cost by her mana cost if " +
        "she entered this turn.)"
    power = 5
    toughness = 4

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.LIFELINK)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{W}{W}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.AddCounters(Counters.INDESTRUCTIBLE, 1, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "11"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb098550-22e6-4079-8c59-ed9ec2f764e3.jpg?1783902976"
    }
}
