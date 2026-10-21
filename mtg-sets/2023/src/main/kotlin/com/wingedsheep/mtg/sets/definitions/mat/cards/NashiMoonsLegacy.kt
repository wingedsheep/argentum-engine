package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nashi, Moon's Legacy — March of the Machine: The Aftermath #39
 * {B}{G}{U} · Legendary Creature — Rat Shaman · 3/4 · Rare
 *
 * Menace, ward {1}
 * Whenever Nashi attacks, exile up to one target legendary or Rat card from your graveyard and
 * copy it. You may cast the copy.
 *
 * The attack trigger is the Kaervek, the Punisher chain: exile the (optional) target, copy it in
 * exile, then `May`-cast the copy paying its normal costs (so {X} is chosen as usual). A declined
 * or unaffordable copy ceases to exist (CR 707.10a); a copy of a permanent spell becomes a token.
 */
val NashiMoonsLegacy = card("Nashi, Moon's Legacy") {
    manaCost = "{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Rat Shaman"
    power = 3
    toughness = 4
    oracleText = "Menace, ward {1}\n" +
        "Whenever Nashi attacks, exile up to one target legendary or Rat card from your graveyard " +
        "and copy it. You may cast the copy. (You still pay its costs. A copy of a permanent spell " +
        "becomes a token.)"

    keywords(Keyword.MENACE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        description = "Whenever Nashi attacks, exile up to one target legendary or Rat card from your " +
            "graveyard and copy it. You may cast the copy."
        val exiledCard = target(
            TargetFilter(
                (GameObjectFilter.Any.legendary() or GameObjectFilter.Any.withSubtype("Rat")).ownedByYou(),
                zone = Zone.GRAVEYARD,
            ),
            optional = true,
        )
        effect = Effects.Pipeline {
            run(Effects.Move(exiledCard, Zone.EXILE))
            val copy = copyCard(exiledCard)
            run(Effects.May(
                Effects.CastFromCollection(copy),
                descriptionOverride = "You may cast the copy.",
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Patrik Hell"
        flavorText = "The story circle continues."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ebd7ce4-550d-41ab-8559-97efb234a0a1.jpg?1783916512"
        ruling("2023-05-12", "You cast the copy while the ability is resolving and still on the stack. You can't wait to cast it later in the turn.")
        ruling("2023-05-12", "Because you're paying the spell's costs, if the spell has {X} in its mana cost, you may choose its value as normal.")
        ruling("2023-05-12", "If you don't want to cast the copy, you can choose not to; the copy ceases to exist the next time state-based actions are performed.")
    }
}
