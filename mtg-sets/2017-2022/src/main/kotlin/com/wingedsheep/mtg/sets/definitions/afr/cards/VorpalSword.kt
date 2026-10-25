package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vorpal Sword (AFR #124) — {B} Artifact — Equipment (rare)
 *
 * Equipped creature gets +2/+0 and has deathtouch.
 * {5}{B}{B}{B}: Until end of turn, this Equipment gains "Whenever equipped creature deals combat
 * damage to a player, that player loses the game."
 * Equip {B}{B}
 *
 * The activated ability grants the quoted trigger to the *Equipment itself*
 * ([Effects.GrantTriggeredAbility] onto [EffectTarget.Self], [Duration.EndOfTurn]) — the Raging
 * Ravine shape. The granted trigger is [Triggers.attached] (ATTACHED binding, the Worldslayer
 * shape), so it watches whichever creature the Sword is attached to when the damage is dealt: move
 * the Sword after activating and the new carrier is the one that ends the game. "That player" is
 * the damaged player ([Player.TriggeringPlayer], the Phage the Untouchable spelling).
 */
val VorpalSword = card("Vorpal Sword") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0 and has deathtouch.\n" +
        "{5}{B}{B}{B}: Until end of turn, this Equipment gains \"Whenever equipped creature deals " +
        "combat damage to a player, that player loses the game.\"\n" +
        "Equip {B}{B}"

    // Equipped creature gets +2/+0 and has deathtouch.
    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.DEATHTOUCH, Filters.EquippedCreature)
    }

    // {5}{B}{B}{B}: Until end of turn, this Equipment gains "Whenever equipped creature deals
    // combat damage to a player, that player loses the game."
    activatedAbility {
        cost = Costs.Mana("{5}{B}{B}{B}")
        effect = Effects.GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer),
                effect = Effects.LoseGame(
                    target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                    message = "Vorpal Sword's equipped creature dealt combat damage"
                ),
                descriptionOverride = "Whenever equipped creature deals combat damage to a player, " +
                    "that player loses the game."
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn
        )
    }

    equipAbility("{B}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7adbe69f-326d-4b4f-850f-c57a1860d17c.jpg?1783926486"
    }
}
