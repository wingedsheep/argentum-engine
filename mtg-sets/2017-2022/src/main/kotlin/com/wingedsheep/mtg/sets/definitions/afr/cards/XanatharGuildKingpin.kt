package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Xanathar, Guild Kingpin
 * {4}{U}{B}
 * Legendary Creature — Beholder
 * 5/6
 * At the beginning of your upkeep, choose target opponent. Until end of turn, that player can't
 * cast spells, you may look at the top card of their library any time, you may play the top card
 * of their library, and you may spend mana as though it were mana of any color to cast spells this
 * way.
 *
 * The permissions are granted to Xanathar's controller (not to Xanathar), so they last the turn
 * even if Xanathar leaves — they come from a resolved ability, not a static one. Both name the
 * target opponent's library through `libraryOwner`, pinned to that player as the grant resolves.
 */
val XanatharGuildKingpin = card("Xanathar, Guild Kingpin") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Beholder"
    power = 5
    toughness = 6
    oracleText = "At the beginning of your upkeep, choose target opponent. Until end of turn, that " +
        "player can't cast spells, you may look at the top card of their library any time, you may " +
        "play the top card of their library, and you may spend mana as though it were mana of any " +
        "color to cast spells this way."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val opponent = target(Targets.Opponent)
        effect = Effects.CantCastSpells(opponent) then
            Effects.GrantStaticAbility(
                ability = LookAtTopOfLibrary(libraryOwner = opponent),
                target = EffectTarget.Controller,
            ) then
            Effects.GrantStaticAbility(
                ability = PlayFromTopWithAlternativeCost(libraryOwner = opponent, withAnyManaColor = true),
                target = EffectTarget.Controller,
            )
        description = "At the beginning of your upkeep, choose target opponent. Until end of turn, " +
            "that player can't cast spells, you may look at the top card of their library any time, " +
            "you may play the top card of their library, and you may spend mana as though it were " +
            "mana of any color to cast spells this way."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "239"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bc58050-a615-4090-9cf5-cd49ccb4e2c6.jpg?1783926441"
        ruling("2021-07-23", "Xanathar doesn't change when you can cast spells or play lands. You must still follow all normal timing rules and land play limits.")
        ruling("2021-07-23", "The target opponent may still cast spells in response to Xanathar, Guild Kingpin's ability in your upkeep. If that player has somehow become an illegal target for the ability as it tries to resolve, the ability does nothing. They may cast spells and you cannot look at or play the top card of their library.")
        ruling("2021-07-23", "During your turn, you can look at the top card of the chosen opponent's library whenever you want, even if you don't have priority.")
        ruling("2021-07-23", "You'll still pay all costs for spells cast this way, including additional costs. You may also pay alternative costs if any are available.")
    }
}
