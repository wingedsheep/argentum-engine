package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellkite Courser — Commander Legends #183
 * {4}{R}{R} · Creature — Dragon 6/5
 *
 * Flying
 * When this creature enters, you may put a commander you own from the command zone onto the
 * battlefield. It gains haste. Return it to the command zone at the beginning of the next end step.
 *
 * The command zone is an ordinary gather source and move destination: "a commander you own from
 * the command zone" is `CardSource.FromZone(Zone.COMMAND, Player.You)` narrowed by
 * `.commander().ownedByYou()`, so an emblem or a Vanguard avatar sharing that zone is never
 * offered, and a game without commanders gathers nothing and the trigger does nothing. With two
 * commanders (partners) the player picks one; `chooseUpTo(1)` is the "may".
 *
 * Putting the commander onto the battlefield is not casting it, so the commander tax doesn't grow
 * (ruling). The haste grant is `Duration.Permanent` on that object — it ends when the object leaves.
 * The return is a per-object delayed trigger created inside the `ForEachInCollection` body, which
 * captures the object's identity (CR 603.7c): if the commander has left the battlefield by the next
 * end step — died, been bounced, or already gone home — it stays where it is (ruling), even if it
 * has come back as a new object since.
 */
val HellkiteCourser = card("Hellkite Courser") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature enters, you may put a commander you own from the command zone onto the " +
        "battlefield. It gains haste. Return it to the command zone at the beginning of the next end step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val commanders = gather(
                CardSource.FromZone(
                    zone = Zone.COMMAND,
                    player = Player.You,
                    filter = GameObjectFilter.Any.commander().ownedByYou(),
                ),
            )
            val chosen = chooseUpTo(
                count = 1,
                from = commanders,
                prompt = "You may put a commander you own from the command zone onto the battlefield",
            )
            val entered = moveTracked(
                from = chosen,
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
            )
            run(
                Effects.ForEachInCollection(
                    entered,
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent) then
                        Effects.CreateDelayedTrigger(
                            step = Step.END,
                            effect = Effects.Move(EffectTarget.IterationEntity, Zone.COMMAND),
                        ),
                ),
            )
        }
        description = "When this creature enters, you may put a commander you own from the command zone " +
            "onto the battlefield. It gains haste. Return it to the command zone at the beginning of the " +
            "next end step."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Caio Monteiro"
        flavorText = "The charge was heralded not by horns, but by the dragon's echoing roar."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db45122e-b5ef-487b-8ea9-59ea066d3c88.jpg?1783928813"
        ruling(
            "2020-11-10",
            "Putting your commander onto the battlefield this way isn't the same as casting that commander. " +
                "The \"commander tax\" won't increase."
        )
        ruling(
            "2020-11-10",
            "You'll return the commander to the command zone only if it's still on the battlefield as the " +
                "delayed triggered ability resolves. If it's in another zone, it will stay where it is."
        )
    }
}
