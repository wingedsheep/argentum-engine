package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Archangel of Wrath
 * {2}{W}{W}
 * Creature — Angel
 * 3/4
 *
 * Kicker {B} and/or {R}
 * Flying, lifelink
 * When this creature enters, if it was kicked, it deals 2 damage to any target.
 * When this creature enters, if it was kicked twice, it deals 2 damage to any target.
 *
 * - "Kicker {B} and/or {R}" is two kicker abilities (CR 702.33b), declared in printed order; each
 *   may be paid at most once, so "kicked twice" means both were paid.
 * - The first trigger's intervening "if" is [Conditions.WasKicked] (either kicker counts); the
 *   second's is both linked kicker slots together, so a doubly-kicked Archangel puts two
 *   independent triggers on the stack, each with its own target.
 */
val ArchangelOfWrath = card("Archangel of Wrath") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "WBR"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Kicker {B} and/or {R} (You may pay an additional {B} and/or {R} as you cast this spell.)\n" +
        "Flying, lifelink\n" +
        "When this creature enters, if it was kicked, it deals 2 damage to any target.\n" +
        "When this creature enters, if it was kicked twice, it deals 2 damage to any target."

    keywordAbility(KeywordAbility.kicker("{B}"))
    keywordAbility(KeywordAbility.kicker("{R}"))
    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(2, victim)
        description = "When this creature enters, if it was kicked, it deals 2 damage to any target."
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.All(
            Conditions.WasKickedWithFirstKicker,
            Conditions.WasKickedWithSecondKicker,
        )
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(2, victim)
        description = "When this creature enters, if it was kicked twice, it deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Miguel Mercado"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d00bab2-e95d-4296-a805-2a05e7640efb.jpg?1783921372"
        ruling("2022-09-09", "If it was kicked twice, each of Archangel of Wrath's last two abilities will trigger once. In other words, it will deal 2 damage to each of two targets or deal 2 damage to the same target twice.")
        ruling("2022-09-09", "You may kick Archangel of Wrath only once for its {B} cost and only once for its {R} cost. You can kick it twice by paying {B}{R}, but you can't kick it twice by paying {B}{B} or {R}{R}. You can't kick it more than twice.")
    }
}
