package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Omnispell Adept — Guilds of Ravnica #49 (canonical / earliest real printing, 2018)
 * {4}{U} · Creature — Human Wizard · 3/4
 *
 * {2}{U}, {T}: You may cast an instant or sorcery spell from your hand without paying its mana cost.
 *
 * Twinning Glass's free-cast pipeline: gather the hand, `chooseSpell` (filter tested against the face
 * that would be cast, so an Adventure's instant/sorcery half qualifies) up to one, and cast it during
 * the ability's resolution — so a sorcery can be cast on an opponent's turn, per the ruling.
 */
val OmnispellAdept = card("Omnispell Adept") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "{2}{U}, {T}: You may cast an instant or sorcery spell from your hand without paying its mana cost."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val spellToCast = chooseSpell(
                from = hand,
                filter = GameObjectFilter.InstantOrSorcery,
                showAllCards = true,
                prompt = "You may cast an instant or sorcery spell from your hand without paying its mana cost",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spellToCast))
        }
        description = "{2}{U}, {T}: Cast an instant or sorcery spell from your hand for free"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Jason Rainville"
        flavorText = "She memorized every arcane text in the Ismeri Library by the time she was sixteen. Now she quizzes sphinxes in her spare time."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e17664df-e8ba-464d-b338-3e671d2d7f0e.jpg?1783934185"
        ruling("2018-10-05", "You may cast a sorcery this way even if it's not your turn.")
        ruling("2018-10-05", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, such as that of Severed Strands, those must be paid to cast the card.")
        ruling("2018-10-05", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
