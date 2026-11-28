package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curse of Echoes
 * {4}{U}
 * Enchantment — Aura Curse
 * Enchant player
 * Whenever enchanted player casts an instant or sorcery spell, each other player may copy that
 * spell and may choose new targets for the copy they control.
 *
 * One loop over the players in turn order starting with the active player (the 2011-01-22 ruling),
 * skipping the caster ([Player.TriggeringPlayer] — the enchanted player, bound at trigger time so
 * it survives the Curse leaving). Inside the loop `Player.You` is the visited player, so the may
 * is theirs and [Effects.CopyTargetSpell]'s default copier makes the copy, and its retarget
 * decision, theirs too. The last copy created resolves first, as the ruling says.
 */
val CurseOfEchoes = card("Curse of Echoes") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\nWhenever enchanted player casts an instant or sorcery spell, each other " +
        "player may copy that spell and may choose new targets for the copy they control."

    auraTarget = Targets.Player

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.If(
                Conditions.Not(Conditions.PlayerIs(Player.You, Player.TriggeringPlayer)),
                Effects.May(
                    Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
                    decisionMaker = EffectTarget.Controller,
                    prompt = "Copy that spell? You may choose new targets for the copy.",
                ),
            ),
        )
        description = "Whenever enchanted player casts an instant or sorcery spell, each other player may " +
            "copy that spell and may choose new targets for the copy they control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "34"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/147dbe42-665a-4e21-b405-d17554d5efcf.jpg?1783940842"

        ruling("2011-01-22", "When the triggered ability resolves, it creates a copy of that spell for each other player. The active player creates their copy and may choose new targets first (if that player isn't the enchanted player), then each other player (except the enchanted player) creates their copy and may choose new targets in turn order. The last player who creates their copy controls the copy that will resolve first.")
        ruling("2011-01-22", "All the copies are created on the stack, so none of them are cast. Abilities that trigger when a player casts a spell won't trigger. The copies will then resolve as normal, after players get a chance to cast spells and activate abilities.")
        ruling("2011-01-22", "If the original spell has an X whose value was determined as it was cast, each copy has the same value of X.")
        ruling("2011-01-22", "You can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
    }
}
