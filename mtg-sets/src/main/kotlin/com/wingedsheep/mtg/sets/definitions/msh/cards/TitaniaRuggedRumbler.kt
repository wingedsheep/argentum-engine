package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Titania, Rugged Rumbler — Marvel Super Heroes #235 (uncommon)
 * {2}{B/G} · Legendary Creature — Human Villain · 5/5
 *
 * As an additional cost to cast this spell, discard a card or pay {2}.
 * Ward—Discard a card or pay {2}.
 *
 * The same printed shape twice, on the two rails that actually differ:
 * - the **cast** side is [Costs.additional.DiscardOrPay] → `AdditionalCost.OrPay`, where the mana
 *   leg folds into the spell's own mana cost at cast time (CR 601.2f), so the enumerator offers a
 *   discard cast and a `{2}`-more cast and the pay path is always available (Pumpkin Bombardment);
 * - the **ward** side is [KeywordAbility.wardDiscardOrPay] → `WardCost.Choice`, a standalone
 *   payment made as the ward trigger resolves (CR 702.21a), so there is no spell cost to fold into
 *   and the mana leg is just another option in the disjunction.
 *
 * The two facades are named to match on purpose: it is one printed wording rendered on each rail,
 * not two inventions. On both sides an opponent with an empty hand is simply offered the pay path
 * only, and declining everything counters their spell.
 */
val TitaniaRuggedRumbler = card("Titania, Rugged Rumbler") {
    manaCost = "{2}{B/G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Human Villain"
    power = 5
    toughness = 5
    oracleText = "As an additional cost to cast this spell, discard a card or pay {2}.\n" +
        "Ward—Discard a card or pay {2}. (Whenever this creature becomes the target of a spell or " +
        "ability an opponent controls, counter it unless that player discards a card or pays {2}.)"

    additionalCost(
        Costs.additional.DiscardOrPay(alternativeManaCost = "{2}")
    )

    keywordAbility(KeywordAbility.wardDiscardOrPay("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Taurin Clarke"
        flavorText = "\"Face it. There's only room for one strongest woman . . . and it ain't you!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c281e0ee-155b-4022-b921-ebc391535aad.jpg?1783902896"
    }
}
