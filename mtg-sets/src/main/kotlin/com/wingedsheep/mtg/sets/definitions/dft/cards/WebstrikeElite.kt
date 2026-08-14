package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Webstrike Elite — Aetherdrift #186
 * {G}{G} · Creature — Insect Archer · 3/3
 *
 * Reach
 * Cycling {X}{G}{G}
 * When you cycle this card, destroy up to one target artifact or enchantment with mana value X.
 *
 * Cycling is an activated ability (CR 702.29a), so X is announced as it's activated (CR 107.3a).
 * The engine carries that announced X on `CardCycledEvent` into the trigger's context, where
 * `manaValueEqualsX()` reads it — so cycling for X=3 can only destroy a mana-value-3 permanent.
 *
 * The cycle payoff is a separate ability from cycling itself (CR 702.29c and the card's rulings):
 * it's legal to cycle with nothing of that mana value on the battlefield, because the target is
 * "up to one" (`optional = true`), and countering one ability leaves the other to resolve.
 */
val WebstrikeElite = card("Webstrike Elite") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Archer"
    power = 3
    toughness = 3
    oracleText = "Reach\n" +
        "Cycling {X}{G}{G} ({X}{G}{G}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, destroy up to one target artifact or enchantment with mana value X."

    keywords(Keyword.REACH)

    keywordAbility(KeywordAbility.cycling("{X}{G}{G}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        val t = target(
            "up to one target artifact or enchantment with mana value X",
            TargetPermanent(
                optional = true,
                filter = TargetFilter(GameObjectFilter.ArtifactOrEnchantment.manaValueEqualsX())
            )
        )
        effect = Effects.Destroy(t)
        description = "When you cycle this card, destroy up to one target artifact or " +
            "enchantment with mana value X."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/064cc22d-d424-4bc9-b8f0-88b170fd6c28.jpg?1783907863"
    }
}
