package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Orah, Skyclave Hierophant
 * {2}{W}{B}
 * Legendary Creature — Kor Cleric
 * 3/3
 *
 * Lifelink
 * Whenever Orah or another Cleric you control dies, return target Cleric card with lesser mana
 * value from your graveyard to the battlefield.
 *
 * "Orah or another Cleric you control" is the ANY binding (Pashalik Mons): Orah is a Cleric, so
 * it sits inside its own trigger's filter, and each Cleric dying at the same time triggers
 * separately (2020-09-25 ruling). "Cleric card" is the bare tribal noun, so the target is any
 * card with the subtype, not only creature cards. "Lesser mana value" is a strict `<` against
 * the dying Cleric's last-known mana value — the Jackdaw Savior shape,
 * `manaValueAtMostDynamic(triggeringManaValue() - 1)`. The target is chosen as the trigger is put
 * on the stack, so a Cleric card that died alongside the triggering one is a legal target
 * (2020-09-25 ruling).
 */
val OrahSkyclaveHierophant = card("Orah, Skyclave Hierophant") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Kor Cleric"
    power = 3
    toughness = 3
    oracleText = "Lifelink\nWhenever Orah or another Cleric you control dies, return target Cleric " +
        "card with lesser mana value from your graveyard to the battlefield."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.CLERIC).youControl()).dies()
        val clericCard = target(
            TargetFilter.CardInGraveyard.withSubtype(Subtype.CLERIC).ownedByYou().manaValueAtMostDynamic(
                DynamicAmounts.triggeringManaValue() - 1
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(clericCard)
        description = "Whenever Orah or another Cleric you control dies, return target Cleric card " +
            "with lesser mana value from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Heonhwa"
        flavorText = "\"The Skyclaves are our inheritance.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2336c8bf-fcad-41f9-9041-80b8aa3cd79a.jpg?1783929321"
        ruling("2020-09-25", "If another Cleric you control dies at the same time as Orah, Orah's ability triggers for that Cleric in addition to triggering for itself.")
        ruling("2020-09-25", "If a creature on the battlefield or a creature card in a graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-09-25", "Orah's ability can target a Cleric card that was put into your graveyard at the same time as the Cleric whose death caused the ability to trigger.")
    }
}
